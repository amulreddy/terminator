package com.autowares.mongoose.camel.processors.gateway;

import java.time.ZonedDateTime;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.autowares.xmlgateway.client.XmlGatewayCommand;
import com.autowares.xmlgateway.model.GatewayOrder;
import com.autowares.xmlgateway.model.GatewayOrderPlacedEvent;
import com.autowares.xmlgateway.model.GenericGatewayOrder;

@Component
public class GatewayFindUnprocessedOrders implements Processor {

	private XmlGatewayCommand xmlGatewayCommand = new XmlGatewayCommand();

	private static Logger log = LoggerFactory.getLogger(GatewayFindUnprocessedOrders.class);

	@Override
	public void process(Exchange exchange) throws Exception {

		GenericGatewayOrder genericGatewayOrder = new GenericGatewayOrder();
		ZonedDateTime startTime = null;
		ZonedDateTime endTime = null;

		try {
			if (exchange.getIn().getBody() != null) {
				genericGatewayOrder.setXmlOrderId(exchange.getIn().getBody(Long.class));
			}
		} catch (Exception e) {
			log.error("Failed to set xmlOrderId.", e);
		}

		if (genericGatewayOrder.getXmlOrderId() == null) {
			genericGatewayOrder.setProcessedFlag(false);
			genericGatewayOrder.setProcessInViper(false);
			startTime = ZonedDateTime.now().minusDays(1);
			endTime = ZonedDateTime.now().minusMinutes(10);
		}

		Page<GatewayOrder> orders = xmlGatewayCommand.findOrders(genericGatewayOrder, startTime, endTime, null);

		Page<GatewayOrderPlacedEvent> events = orders.map(i -> {
			GatewayOrderPlacedEvent event = new GatewayOrderPlacedEvent();
			event.setOrder(i);
			return event;
		});

		exchange.getIn().setBody(events.getContent());
	}

}
