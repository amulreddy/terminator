package com.autowares.mongoose.camel.processors.nonstock;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.apis.ids.command.LocationLookupClient;
import com.autowares.apis.ids.model.BusinessDetail;
import com.autowares.apis.ids.model.LocationLookupRequest;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.BusinessContext;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorContextImpl;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.CoordinatorOrderContextImpl;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.mongoose.model.ProcurementGroupContext;
import com.autowares.mongoose.model.TransactionalContext;
import com.autowares.mongoose.service.SequenceGeneratorService;
import com.autowares.partyconfiguration.model.PartnershipType;
import com.autowares.servicescommon.model.LocationType;
import com.autowares.servicescommon.model.PurchaseOrderType;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.supplychain.model.ProcurementGroup;
import com.autowares.supplychain.model.TransactionContext;
import com.autowares.supplychain.model.TransactionScope;

@Component
public class BuildNonStockContexts implements Processor {

	LocationLookupClient idsClient = new LocationLookupClient();
	Logger log = LoggerFactory.getLogger(BuildNonStockContexts.class);
	
	@Autowired
	private SequenceGeneratorService sequenceGeneratorService;

	@Override
	public void process(Exchange exchange) throws Exception {

		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);

		log.info("BuildNonStockContexts from ProdLine");
		Map<Long, List<LineItemContext>> supplierMap = context.getLineItems().stream()
				.filter(i -> i.getPrimarySupplierId() != null)
				.collect(Collectors.groupingBy(LineItemContext::getPrimarySupplierId));
		for (Entry<Long, List<LineItemContext>> entry : supplierMap.entrySet()) {
			String key = String.valueOf(entry.getKey());
			BusinessDetail businessDetail = null;

			try {
				LocationLookupRequest request = new LocationLookupRequest();
				request.setBusEntId(key);
				businessDetail = idsClient.findBusinessLocation(request);
			} catch (Exception e) {

			}
			if (businessDetail == null) {
				context.updateProcessingLog("Unable to resolve supplier business entity for VCSC " + key);
				return;
			}

			Optional<FulfillmentLocationContext> existingFulfillmentFromSupplier = context.getFulfillmentSequence()
					.stream().filter(i -> key.equals(i.getLocation())).findAny();

			if (existingFulfillmentFromSupplier.isPresent()) {
				log.info("We have existing supplier");
				break;
			}
			
			CoordinatorContext nonStockContext;
			if (context instanceof CoordinatorOrderContext) {
				nonStockContext = new CoordinatorOrderContextImpl();
			} else {
				nonStockContext = new CoordinatorContextImpl();
			}
			String documentId = sequenceGeneratorService.getNextSequence();
			nonStockContext.setDocumentId(documentId);
			FulfillmentLocationContext fulfillmentLocation = new FulfillmentLocationContext(nonStockContext, key);
			fulfillmentLocation.setNonStockContext(nonStockContext);
			context.getFulfillmentSequence().add(fulfillmentLocation);
//			Set transaction to prevent being picked up by coordinator packslip processing
			fulfillmentLocation.setTransactionStage(TransactionStage.Open);
			fulfillmentLocation.setTransactionStatus(TransactionStatus.Pending);
			fulfillmentLocation.setLocationType(LocationType.Vendor);
			ProcurementGroup pg = null;
			Optional<ProcurementGroup> existingGroup = context.getProcurementGroups().stream()
					.filter(i -> key.equals(i.getProcurementGroupName())).findAny();
			if (existingGroup.isPresent()) {
				pg = existingGroup.get();
				nonStockContext.getProcurementGroups().add(pg);
			} else {
				if(context.getProcurementGroupContext() != null) {
					pg = context.getProcurementGroupContext();
					nonStockContext.getProcurementGroups().add(pg);
				}
				if(pg == null) {
					pg = ProcurementGroup.builder().withPartnershipType(PartnershipType.Procuring)
							.withProcumentGroupId(UUID.randomUUID()).withProcurementGroupName(key).build();
					context.getProcurementGroups().add(pg);
					nonStockContext.getProcurementGroups().add(pg);
				}
			}
			
			ProcurementGroupContext procurementGroupContext = null;
			TransactionContext supplierTransactionContext = null;
			
			if(pg instanceof ProcurementGroupContext) {
				procurementGroupContext = (ProcurementGroupContext)pg;
				supplierTransactionContext = procurementGroupContext.getSupplierContext();
			} else {
				procurementGroupContext = new ProcurementGroupContext(pg);
				supplierTransactionContext = TransactionContext.builder().withTransactionReferenceId(documentId).withTransactionScope(TransactionScope.Purchasing)
						.build();
				procurementGroupContext.setSupplierContext(new TransactionalContext(supplierTransactionContext));
				procurementGroupContext.setCustomerContext(context.getTransactionContext());
			}
			
			if (context instanceof CoordinatorOrderContext) {
				procurementGroupContext.getSupplierContext().setOrderContext(nonStockContext);
				procurementGroupContext.getCustomerContext().setOrderContext(context);
			} else {
				procurementGroupContext.getSupplierContext().setQuoteContext(nonStockContext);
				procurementGroupContext.getCustomerContext().setQuoteContext(context);
			}
			nonStockContext.setProcurementGroupContext(procurementGroupContext);
			if (context instanceof CoordinatorContextImpl) {
				CoordinatorContextImpl impl = (CoordinatorContextImpl) context;
				impl.setProcurementGroupContext(procurementGroupContext);
			}

			nonStockContext.getFulfillmentSequence().add(fulfillmentLocation);

			nonStockContext.setTransactionContext(supplierTransactionContext);
			BusinessContext account = new BusinessContext();
			BusinessDetail awi = new BusinessDetail();
			awi.setBusEntId(4836l);
			awi.setBusinessName("Auto-Wares Inc");
			account.setBusinessDetail(awi);
			nonStockContext.setBusinessContext(account);
			BusinessContext supplier = new BusinessContext();
			supplier.setBusinessDetail(businessDetail);
			nonStockContext.setShipTo(context.getBusinessContext());
			nonStockContext.setSupplier(supplier);
			nonStockContext.setOrderType(PurchaseOrderType.SpecialOrder);
			for (LineItemContext mainLineItem : entry.getValue()) {
				LineItemContextImpl nonStockLineItem = new LineItemContextImpl(nonStockContext, mainLineItem);
				Availability mainAvailability = new Availability(mainLineItem, fulfillmentLocation, nonStockLineItem);
				Availability nonStockAvailability = new Availability(nonStockLineItem, fulfillmentLocation,
						mainLineItem);
				nonStockLineItem.setPart(mainLineItem.getPart());
				nonStockContext.getLineItems().add(nonStockLineItem);
			}
		}

		if (supplierMap.isEmpty()) {
			log.warn("No Prodline data found");
		}
	}

}
