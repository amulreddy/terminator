package com.autowares.mongoose.camel.processors.gateway;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.xmlgateway.client.XmlGatewayCommand;
import com.autowares.xmlgateway.model.GatewayOrder;

@Component
public class GatewaySetProcessFlag implements Processor {

	private final static XmlGatewayCommand xmlGatewayCommand = new XmlGatewayCommand();
	private static Logger log = LoggerFactory.getLogger(GatewaySetProcessFlag.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		GatewayOrder orderInput = exchange.getIn().getHeader("gatewayOrder", GatewayOrder.class);
		CoordinatorOrderContext orderContext = exchange.getIn().getBody(CoordinatorOrderContext.class);
		if (orderContext != null && orderContext.getXmlOrderId() != null && orderInput!= null) {
			
			Long xmlOrderId = null;
			try {
				xmlOrderId = Long.parseLong(orderContext.getXmlOrderId());
			} catch (NumberFormatException e) {
				return;
			}
			Boolean processedByCoordinator = true;
			Boolean processInViper = false;
			boolean persistedInMoa = false;
			
			if (exchange.getIn().getHeader("persistedInMoa") != null) {
			     persistedInMoa = true;
			}
			if (TransactionStatus.Error == orderContext.getTransactionStatus()) {
				if (persistedInMoa) {
					log.error("Order already persisted in MOA, needs to be dealt with");
				} else {
					processedByCoordinator = false;
					processInViper = true;
				}
			}
			xmlGatewayCommand.setProcessFlag(xmlOrderId, processedByCoordinator, processInViper);

		}
	}

}
