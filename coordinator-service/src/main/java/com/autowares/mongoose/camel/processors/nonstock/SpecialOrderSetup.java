package com.autowares.mongoose.camel.processors.nonstock;

import java.util.List;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.apis.partservice.PartAvailability;
import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.mongoose.model.ProcurementGroupContext;
import com.autowares.servicescommon.model.Freight;
import com.autowares.servicescommon.model.FreightOptions;
import com.autowares.servicescommon.model.LocationType;
import com.autowares.servicescommon.model.PurchaseOrderType;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.supplychain.model.SupplyChainSourceDocument;

@Component
public class SpecialOrderSetup implements Processor {

	private static Logger log = LoggerFactory.getLogger(SpecialOrderSetup.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);

		log.info("Special Orders from ProdLine");

		for (FulfillmentLocationContext fulfillmentLocation : context.getFulfillmentSequence()) {
			if(!LocationType.Vendor.equals(fulfillmentLocation.getLocationType())) {
				log.info("Location type is not vendor, ignoring");
				break;
			}
			if (context.getProcurementGroupContext().getCustomerContext().getQuoteContext() != null) {
				fulfillmentLocation.setQuoted(true);
			}

			ProcurementGroupContext pg = context.getProcurementGroupContext();

			context.getProcurementGroups().add(pg);
			if (context.getInquiryOptions() != null) {
				context.getInquiryOptions().setUseFlatRateShipping(true);
			}
			if (context.getTransactionContext() != null && context.getTransactionContext().getRequest() != null
					&& context.getTransactionContext().getRequest().getInquiryOptions() != null) {
				context.getTransactionContext().getRequest().getInquiryOptions().setUseFlatRateShipping(true);
			}
			CoordinatorContext nonStockContext = fulfillmentLocation.getNonStockContext();
			
			ProcurementGroupContext procurementGroupContext = pg;
			nonStockContext.setOrderType(PurchaseOrderType.SpecialOrder);
			nonStockContext.setSystemType(SystemType.VIC);
			fulfillmentLocation.setSystemType(SystemType.VIC);
			
			// TODO: Move this to a different processor
			for(LineItemContextImpl line : context.getLineItems()) {
				List<LineItemContextImpl> quotedLines = procurementGroupContext.getCustomerContext().getQuoteContext().getLineItems();
				Optional<LineItemContextImpl> matchedLine = quotedLines.stream().filter(l -> l.getLineNumber().equals(line.getLineNumber())).filter(l -> l.getProductId().equals(line.getProductId())).findAny();
				if(!matchedLine.isPresent()) {
					throw new AbortProcessingException("Order request contains parts that were not quoted.");
				}
			}

			// TODO: Move this to a different processor
			for(LineItemContextImpl line : procurementGroupContext.getCustomerContext().getQuoteContext().getLineItems()) {
				List<LineItemContextImpl> requestLines = context.getLineItems();
				Optional<LineItemContextImpl> matchedLine = requestLines.stream().filter(l -> l.getLineNumber().equals(line.getLineNumber())).filter(l -> l.getProductId().equals(line.getProductId())).findAny();
				if(!matchedLine.isPresent()) {
					throw new AbortProcessingException("Quote request contains parts that were not ordered.");
				}
			}

			for (SupplyChainSourceDocument shippingEstimate : procurementGroupContext.getSupplierContext().getShippingEstimates()) {
				FreightOptions freightOptions = new FreightOptions();
				if(shippingEstimate.getFreightOptions() != null) {
					if(shippingEstimate.getFreightOptions().getSelectedFreight() != null) {
						Freight selectedFreight = copyFreight(shippingEstimate.getFreightOptions().getSelectedFreight());
						freightOptions.setSelectedFreight(selectedFreight);
					}
					if(shippingEstimate.getFreightOptions().getAvailableFreight() != null) {
						freightOptions.setAvailableFreight(shippingEstimate.getFreightOptions().getAvailableFreight().stream().map(f -> copyFreight(f)).toList());
						
					}
				}
				
				nonStockContext.setFreightOptions(freightOptions);
				context.setFreightOptions(freightOptions);
//				
//				for(SupplyChainLine line : shippingEstimate.getLineItems()) {
//				List<LineItemContextImpl> requestLines = context.getLineItems();
//				Optional<LineItemContextImpl> matchedLine = requestLines.stream().filter(l -> l.getLineNumber().equals(line.getLineNumber())).filter(l -> l.getProductId().equals(line.getProductId())).findAny();
//				if(!matchedLine.isPresent()) {
//					PartAvailability partAvailability = new PartAvailability();
////					partAvailability.setQuantityOnHand(line.getQuantity()); // Don't hard-code
//					
//					partAvailability.setQuantityOnHand(1); // Don't hard-code
//					
//					partAvailability.setBuildingCode(fulfillmentLocation.getLocation());
//					partAvailability.setSystemType(shippingEstimate.getSystemType());
//					matchedLine.get().getPart().getAvailability().add(partAvailability);
//				}
//				}
			}
			
			
			
			for(Availability availability: fulfillmentLocation.getLineItemAvailability()) {
				PartAvailability partAvailability = new PartAvailability();
				partAvailability.setQuantityOnHand(availability.getMatchingLineItem().getOriginalQuantity()); // Don't hard-code
				partAvailability.setBuildingCode(fulfillmentLocation.getLocation());
				partAvailability.setSystemType(SystemType.VIC); // Don't hard-code
				availability.getLineItem().getPart().getAvailability().add(partAvailability);
				availability.getMatchingLineItem().setPart(availability.getLineItem().getPart());
			}
		}

		context.getFulfillmentOptions().setAllowSupplierInventory(true);

	}

	private Freight copyFreight(Freight selectedFreight) {
		
		Freight freight = new Freight();
		freight.setAvailableQuantity(selectedFreight.getAvailableQuantity());
		freight.setCarrier(selectedFreight.getCarrier());
		freight.setDescription(selectedFreight.getDescription());
		freight.setCost(selectedFreight.getCost());
		freight.setCarrierCode(selectedFreight.getCarrierCode());
		freight.setDeliveryMethod(selectedFreight.getDeliveryMethod());
		freight.setDeliveryRunName(selectedFreight.getDeliveryRunName());
		freight.setDeliveryRunType(selectedFreight.getDeliveryRunType());
		freight.setDocumentReferenceId(selectedFreight.getDocumentReferenceId());
		freight.setDocumentReferenceIds(selectedFreight.getDocumentReferenceIds());
		freight.setEstimatedDeliveryDateTime(selectedFreight.getEstimatedDeliveryDateTime());
		freight.setEstimatedShipDateTime(selectedFreight.getEstimatedShipDateTime());
		freight.setExpireTime(selectedFreight.getExpireTime());
		
		freight.setHandlingPriorityType(selectedFreight.getHandlingPriorityType());
		freight.setLocationName(selectedFreight.getLocationName());
		freight.setLocationType(selectedFreight.getLocationType());
		freight.setPlannedFillQuantity(selectedFreight.getPlannedFillQuantity());
		freight.setPrice(selectedFreight.getPrice());
		freight.setProcurementGroupId(selectedFreight.getProcurementGroupId());
		freight.setReceivedTime(selectedFreight.getReceivedTime());
		freight.setSequence(selectedFreight.getSequence());
		freight.setServiceClass(selectedFreight.getServiceClass());
		freight.setShippingCode(selectedFreight.getShippingCode());
		freight.setShippingMethod(selectedFreight.getShippingMethod());
		freight.setShipQuantity(selectedFreight.getShipQuantity());
		freight.setStockedTime(selectedFreight.getStockedTime());
		freight.setStockQuantity(selectedFreight.getStockQuantity());
		freight.setSystemType(selectedFreight.getSystemType());
		freight.setTrackingNumber(selectedFreight.getTrackingNumber());
		freight.setWarehouseNumber(selectedFreight.getWarehouseNumber());
		
		return freight;
	}

}
