package com.autowares.mongoose.camel.processors.integration;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.MoaOrderContext;
import com.autowares.mongoose.service.ViperToWmsCoreUtils;
import com.autowares.orders.model.Item;
import com.autowares.servicescommon.model.LocationType;
import com.autowares.servicescommon.model.OrderSource;
import com.autowares.servicescommon.model.RunType;
import com.autowares.servicescommon.model.SystemType;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ViperCoordinatorContext implements Processor {

	private Logger log = LoggerFactory.getLogger(ViperCoordinatorContext.class);

	@Autowired
	private ViperToWmsCoreUtils viperToWmsCoreUtils;

	@SuppressWarnings("rawtypes")
	@Override
	public void process(Exchange exchange) throws Exception {
		MoaOrderContext orderContext = null;
		try {
			Map orderUpdateEvent = new ObjectMapper().convertValue(exchange.getIn().getBody(), Map.class);

			Item orderItem = null;

			if (orderUpdateEvent.containsKey("orderID")) {
				orderItem = viperToWmsCoreUtils.lookupProdOrderByDetails((long) (int) orderUpdateEvent.get("orderID"));
			}

			if (orderItem == null) {
				log.error("Failed to find orderItem");
				throw new AbortProcessingException();
			}

			orderItem = viperToWmsCoreUtils.resolveOrderId(orderItem);

			if (orderItem == null) {
				log.error("Failed to find orderItem");
				throw new AbortProcessingException();
			}

			orderItem.getOrder().getItems().add(orderItem);
			orderContext = new MoaOrderContext(orderItem.getOrder());
			RunType run = RunType.find(orderItem.getOrderType() + "");
			orderContext.setDeliveryMethod(run.getDeliveryMethod());
			orderContext.setServiceClass(run.getServiceClass());
			orderContext.getInquiryOptions().setLookupSource(orderItem.getOrder().getOrderSource());
			if (orderContext.getInquiryOptions().getLookupSource() == null) {
				orderContext.getInquiryOptions().setLookupSource(OrderSource.ViperProgram2F);
			}

			FulfillmentLocationContext fulfillment = new FulfillmentLocationContext(orderContext,
					orderItem.getBuilding());
			fulfillment.setSystemType(SystemType.AwiWarehouse);
			fulfillment.setLocationType(LocationType.Warehouse);
			fulfillment.setDocumentId(orderContext.getDocumentId());

			Availability availability = new Availability(orderContext.getLineItems().get(0), fulfillment);
			availability.setMoaOrderDetail(orderItem);
			availability.setFillQuantity(orderItem.getShipQuantity());
			fulfillment.getLineItemAvailability().add(availability);
			orderContext.getFulfillmentSequence().add(fulfillment);

			if (orderContext != null) {
				log.trace("We have a context.");
			}

			String action = (String) orderUpdateEvent.get("action");
			if (action == null) {
				action = (String) orderUpdateEvent.get("status");
			}

			String label = (String) orderUpdateEvent.get("toteID");
			if (label == null) {
				label = (String) orderUpdateEvent.get("labelID");
			}

			String employeeId = (String) orderUpdateEvent.get("employeeNumber");
			String employeeName = (String) orderUpdateEvent.get("employeeName");
			Integer quantity = (Integer) orderUpdateEvent.get("quantity");
			if (quantity == null) {
				quantity = (Integer) orderUpdateEvent.get("packageQuantity");
			}
			log.debug(orderUpdateEvent.get("eventType") + " '" + action + "' barcode: '" + label + "' JobFlag:'"
					+ orderUpdateEvent.get("jobFlag") + "' " + employeeId + " '" + employeeName + "'" + " qty: "
					+ quantity);

		} catch (AbortProcessingException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
		}

		exchange.getIn().setHeaders(exchange.getIn().getHeaders());
		exchange.getIn().setBody(orderContext);

	}

}
