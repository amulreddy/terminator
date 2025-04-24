package com.autowares.mongoose.camel.processors.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.apis.ids.model.BusinessDetail;
import com.autowares.apis.partservice.Part;
import com.autowares.apis.partservice.PartAvailability;
import com.autowares.labelservice.model.PrintNowSlipRequest;
import com.autowares.labelservice.model.ShipperPart;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.FulfillmentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.servicescommon.model.DeliveryMethod;

@Component
public class PrintOrder implements Processor {

	// This is for in warehouse stores.
	private static Logger log = LoggerFactory.getLogger(PrintOrder.class);

	@Override
	public void process(Exchange exchange) throws Exception {

		log.info("Printing for in warehouse store.");
		CoordinatorOrderContext context = exchange.getIn().getBody(CoordinatorOrderContext.class);
		exchange.getIn().setHeaders(exchange.getIn().getHeaders());

		for (FulfillmentLocationContext fillContext : context.getFulfillmentSequence()) {

			PrintNowSlipRequest printNowSlipRequest = new PrintNowSlipRequest();
			BusinessDetail businessDetail = context.getBusinessContext().getBusinessDetail();
			printNowSlipRequest.setCustomerName(businessDetail.getBusinessName());
			printNowSlipRequest.setCustomerLine2(businessDetail.getAddress());
			printNowSlipRequest.setCustomerLine3(businessDetail.getCity() + ", " + businessDetail.getStateProv() + " "
					+ businessDetail.getPostalCode());
			printNowSlipRequest.setCustRunDestBldg(businessDetail.getServicingWarehouse().getBuildingMnemonic());
			printNowSlipRequest
					.setDateTimestamp(LocalTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss")));
			printNowSlipRequest.setDeliveryDescription(DeliveryMethod.CustomerPickUp.toString());
			printNowSlipRequest.setFloorLocation(" "); // Pull out of core using truck run?
			printNowSlipRequest.setShippingLaneTarget(null);
			printNowSlipRequest.setPurchaseOrder(context.getPurchaseOrder());
			printNowSlipRequest.setOneCopy(true);
			printNowSlipRequest.setShipperBarCode(null); // Core container id? How to link printed slip to actual list
															// of order details?
			printNowSlipRequest.setShipperNumber("Julian Date" + "4 digit serial reset daily"); // Generate unique job
																								// id in label print
																								// service
			// TODO complete printNowSlipRequest for in warehouse store
			for (FulfillmentContext fulfillmentDetail : fillContext.getFulfillmentDetails()) {
				if (fulfillmentDetail.getFillQuantity() > 0) {
					Part part = fulfillmentDetail.getLineItem().getPart();
					Availability availability = fulfillmentDetail.getAvailability();
					ShipperPart shipperPart = new ShipperPart();
					shipperPart.setBillingPrice(String.valueOf(availability.getLineItem().getPrice()));
					shipperPart.setCorePrice(String.valueOf(part.getCorePrice()));
					shipperPart.setOrderQuantity(String.valueOf(fulfillmentDetail.getLineItem().getQuantity()));
					// TODO Coordinator - Adjust warehouse to fill quantity based on CW Multiplier
					shipperPart.setPartDescription(part.getShortDescription());
					shipperPart.setPartNumber(part.getPartNumber());
					shipperPart.setShipQuantity(String.valueOf(fulfillmentDetail.getFillQuantity()));
					shipperPart.setuOM(part.getUnitOfMeasure());
					shipperPart.setVendorSubCode(part.getVendorCodeSubCode());
					if (availability != null) {
						PartAvailability partAvailability = availability.getPartAvailability();
						shipperPart.setZone(String.valueOf(partAvailability.getZone()));
						shipperPart.setAisle(partAvailability.getAisle());
						shipperPart.setRack(partAvailability.getRack());
					}
					printNowSlipRequest.setDate(context.getOrderTime().format(DateTimeFormatter.ofPattern("MM/dd")));
					printNowSlipRequest
							.setKcmlTime(context.getOrderTime().format(DateTimeFormatter.ofPattern("hh:mm:ss")));

					printNowSlipRequest.getShipperParts().add(shipperPart);
				}
			}
		}

		// TODO - Bulk parts -> print to label one label per item
		// TODO - Bin parts -> print on paper slip w/ header, n items, footer

//		PrintJobImpl printJob = new PrintJobImpl();

	}

}
