package com.autowares.mongoose.camel.processors.lookup;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.apis.ids.model.BusinessDetail;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.partyconfiguration.client.PartyConfigurationClient;
import com.autowares.partyconfiguration.model.Configuration;
import com.autowares.partyconfiguration.model.DeliverySetting;
import com.autowares.partyconfiguration.model.SupplyChain;
import com.autowares.servicescommon.model.DeliveryMethod;
import com.autowares.servicescommon.model.FulfillmentOptions;
import com.autowares.servicescommon.model.LocationType;
import com.autowares.servicescommon.model.OrderSource;
import com.autowares.servicescommon.model.ServiceClass;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.supplychain.model.TransactionContext;
import com.autowares.supplychain.model.TransactionScope;

@Component
public class PartyConfigurationLookup implements Processor {

	PartyConfigurationClient partyClient = new PartyConfigurationClient();
	Logger log = LoggerFactory.getLogger(PartyConfigurationLookup.class);

	@Override
	public void process(Exchange exchange) throws Exception {

		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);
		OrderSource lookupSource = context.getInquiryOptions().getLookupSource();
		SystemType lookupSystemType = SystemType.AwiWarehouse;
		if (lookupSource != null) {
			lookupSystemType = lookupSource.getSystemType();
		} else {
			log.error("null LookupSource in inquiryOptions!, could result in incorrect configuration");
		}
		BusinessDetail customerDetails = context.getBusinessContext().getBusinessDetail();
		Long customerBusinessEntityId = customerDetails.getBusEntId();
		Long autowaresBusinessEntityId = 4836l;

		TransactionContext transactionContext = context.getTransactionContext();
		if (transactionContext == null) {
			log.warn("Null transaction context, creating one");
			transactionContext = TransactionContext.builder().build();
			context.setTransactionContext(transactionContext);
		}

		SupplyChain supplyChain = transactionContext.getSupplyChain();

		if (supplyChain == null) {
			Long supplier = 4656L;
			if (customerDetails.getServicingWarehouse() != null) {
				supplier = customerDetails.getServicingWarehouse().getBusinessEntityId();
			} else {
				log.warn("No servicing warehouse found for account " + customerDetails.getAwiAccountNo()
						+ ".  Defaulting to warehouse 01.");
			}
			Collection<SupplyChain> supplyChains = partyClient.findSupplyChains(Set.of(customerBusinessEntityId),
					Set.of(supplier), null);
			supplyChain = resolveSupplyChain(supplyChains, lookupSystemType);
		}

		if (supplyChain != null) {
			if (supplyChain.getProcuringPartnership() != null) {
				transactionContext.setTransactionScope(TransactionScope.Purchasing);
			} else {
				transactionContext.setTransactionScope(TransactionScope.Supplying);
			}
			transactionContext.setSupplyChain(supplyChain);
			Configuration configuration = supplyChain.getConfiguration();
			if (configuration != null) {
				context.setConfiguration(configuration);
				if (configuration.getSettings() != null) {
					exchange.getProperties().putAll(configuration.getSettings());
				}
				FulfillmentOptions fulfillmentOptions = context.getFulfillmentOptions();
				// Configured preferred fulfillment locations
				if (!lookupSystemType.equals(supplyChain.getUltimateProcurer().getSystemType())) {
					//  Remove delivery settings if System Types don't match.
					//  Delivery settings are only applied for MyPlace configs.
					//  This is being done for Highline Warren (See WMS-210).
					configuration.setDeliveryPreference(null);
					configuration.setDeliverySettings(null);
				}
				if (configuration.getDeliverySettings() != null) {
					for (DeliverySetting setting : configuration.getDeliverySettings()) {
						if(DeliveryMethod.CustomerPickUp == setting.getDeliveryPreference() ) {
							fulfillmentOptions.getPreferredLocations().add(setting.getLocation());
						}
					}
				}
				// Configured delivery preference
				if (configuration.getDeliveryPreference() != null) {
					context.setDeliveryMethod(configuration.getDeliveryPreference());
				}
			}
		}

		log.info("Populate NonStock Contexts");
		for (FulfillmentLocationContext fulfillmentLocation : context.getFulfillmentSequence()) {
			if (LocationType.Vendor.equals(fulfillmentLocation.getLocationType())) {
				CoordinatorContext nonStockContext = fulfillmentLocation.getNonStockContext();

				if (nonStockContext.getSupplier() != null && nonStockContext.getSupplier().getBusinessDetail() != null
						&& nonStockContext.getSupplier().getBusinessDetail().getBusEntId() != 0) {

					Long supplierBusEntId = nonStockContext.getSupplier().getBusinessDetail().getBusEntId();

					Collection<SupplyChain> supplyChains = partyClient
							.findSupplyChains(Set.of(customerBusinessEntityId), Set.of(supplierBusEntId), null);
					
					//TODO Temporary testing code.
					lookupSystemType=SystemType.VIC;
					
					SupplyChain nonStockSupplyChain = resolveSupplyChain(supplyChains, lookupSystemType);
					if (nonStockSupplyChain != null && nonStockSupplyChain.getProcuringSystem() != null) {
						nonStockContext.getTransactionContext().setSupplyChain(nonStockSupplyChain);
						fulfillmentLocation.setSystemType(nonStockSupplyChain.getProcuringSystem().getSystemType());
						// For now, so customer quote is not set to manual - what do we do with multiple
						// system types?
						nonStockContext.setSystemType(nonStockSupplyChain.getProcuringSystem().getSystemType());
					} else {
						nonStockContext.setSystemType(SystemType.Manual);
						fulfillmentLocation.setSystemType(SystemType.Manual);
					}

				}
			}
		}

	}

	private SupplyChain resolveSupplyChain(Collection<SupplyChain> supplyChains, SystemType lookupSystemType) {

		Optional<SupplyChain> optionalSupplyChain = Optional.empty();

		if (lookupSystemType != null) {
			optionalSupplyChain = supplyChains.stream().sorted(resultComparator(lookupSystemType)).findFirst();
		}

		if (optionalSupplyChain.isPresent()) {
			return optionalSupplyChain.get();
		}
		return null;
	}

	private Comparator<SupplyChain> resultComparator(SystemType systemType) {

		Comparator<SupplyChain> systemTypeComparator = Comparator.comparing(SupplyChain::getUltimateProcurer,
				(s1, s2) -> {
					if (s1 == s2) {
						return 0;
					}
					if (s1 != null && s1.getSystemType().equals(systemType)) {
						return -1;
					}
					return 0;
				});

		return systemTypeComparator;
	}
}
