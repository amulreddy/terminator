package com.autowares.mongoose.camel.processors.conditiondetection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.apis.partservice.PartAvailability;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.servicescommon.model.DeliveryMethod;
import com.autowares.servicescommon.model.LocationType;
import com.autowares.servicescommon.model.PurchaseOrderType;
import com.autowares.servicescommon.model.ServiceClass;
import com.autowares.servicescommon.model.SourceDocumentType;

@Component
public class DropShipDetectionProcessor implements Processor {

	private static Logger log = LoggerFactory.getLogger(DropShipDetectionProcessor.class);
	
	private final String HIGHLINE_COMMON_NAME = "296904";

	@Override
	public void process(Exchange exchange) throws Exception {
		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);

		String purchaseOrder = context.getCustomerDocumentId();
		if (purchaseOrder != null && purchaseOrder.startsWith("DS")) {
			context.setOrderType(PurchaseOrderType.DropShip);
			log.info(PurchaseOrderType.DropShip + " detected for xmlOrderId: " + context.getDocumentId());
		} else {
			List<FulfillmentLocationContext> suppliers = context.getFulfillmentSequence().stream()
//					.filter(i -> i.getLocation().equals(HIGHLINE_COMMON_NAME))
					.filter(i -> LocationType.Vendor.equals(i.getLocationType())).collect(Collectors.toList());

			if (suppliers.size() == 1) {
				FulfillmentLocationContext fulfillmentLocationContext = suppliers.get(0);
				if (fulfillmentLocationContext.getLocation().equals(HIGHLINE_COMMON_NAME)) {
					if (exchange.getProperty("dropShipTesting", false, Boolean.class)) {
						if ((ServiceClass.BestEffort.equals(context.getServiceClass())
								&& !DeliveryMethod.CustomerPickUp.equals(context.getDeliveryMethod()))) {
							//  Check to see if any of the Highline line items are set as MustGo.
							Optional<LineItemContext> optionalLineItem = fulfillmentLocationContext
									.getLineItemAvailability().stream().map(i -> i.getLineItem())
									.filter(i -> i.getMustGo() !=null && i.getMustGo()).findAny();
							if (!optionalLineItem.isPresent()) {
//								
								if (SourceDocumentType.PurchaseOrder.equals(context.getSourceDocumentType())) {
									context.getFulfillmentOptions().getPreferredLocations().add(HIGHLINE_COMMON_NAME);
									List<LineItemContextImpl> location = context.getLineItems().stream()
										    .filter(i -> i.getAvailability() != null && !i.getAvailability().isEmpty() 
										            && i.getAvailability().get(0).getFulfillmentLocation() != null 
										            && i.getAvailability().get(0).getFulfillmentLocation().getLocation() != null 
										            && i.getAvailability().get(0).getFulfillmentLocation().getLocation().equals(HIGHLINE_COMMON_NAME))
										    .collect(Collectors.toList());

									for (LineItemContextImpl lineItem : location) {

										PartAvailability highLinePartAvailability = new PartAvailability();
										highLinePartAvailability.setQuantityOnHand(lineItem.getQuantity());
										highLinePartAvailability.setBuildingCode(HIGHLINE_COMMON_NAME);
										if (lineItem.getPart() != null) {
											if (lineItem.getPart().getAvailability() == null) {
												lineItem.getPart().setAvailability(new ArrayList<>());
											}
											lineItem.getPart().getAvailability().add(highLinePartAvailability);
										}
									}
//									
								}
								// setOrderType previously in above if block.
								context.setOrderType(PurchaseOrderType.DropShip);
								exchange.setProperty("dropShipAllowed", true);
								
								log.info(PurchaseOrderType.DropShip + " detected for xmlOrderId: "
										+ context.getDocumentId());
							}
						}
					}
				}
			}
		}
	}
}
