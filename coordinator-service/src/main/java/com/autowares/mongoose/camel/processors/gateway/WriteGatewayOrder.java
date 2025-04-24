package com.autowares.mongoose.camel.processors.gateway;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.xmlgateway.client.XmlGatewayCommand;
import com.autowares.xmlgateway.model.GatewayOrder;

@Component
public class WriteGatewayOrder implements Processor {
	
	private final static XmlGatewayCommand xmlGatewayCommand = new XmlGatewayCommand();
	private static Logger log = LoggerFactory.getLogger(WriteGatewayOrder.class);
	

	@Override
	public void process(Exchange exchange) throws Exception {
		
		CoordinatorOrderContext orderContext = exchange.getIn().getBody(CoordinatorOrderContext.class);
		if (orderContext.getXmlOrderId() != null) {
			if ("Prod".equals(exchange.getIn().getHeader("activeMqSource"))) {
				GatewayOrder order = exchange.getIn().getHeader("gatewayOrder", GatewayOrder.class);
				if (order != null) {
					log.info("Writing order: " + orderContext.getXmlOrderId() + " to the gateway");
					xmlGatewayCommand.placeOrder(order, false);
				}
			}
		}
	}

}
