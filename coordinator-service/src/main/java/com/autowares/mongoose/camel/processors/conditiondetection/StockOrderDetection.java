package com.autowares.mongoose.camel.processors.conditiondetection;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.servicescommon.model.ServiceClass;
import com.autowares.xmlgateway.model.GatewayOrder;

@Component
public class StockOrderDetection implements Processor {

	private static Logger log = LoggerFactory.getLogger(StockOrderDetection.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);
		GatewayOrder gatewayOrder = exchange.getIn().getHeader("gatewayOrder", GatewayOrder.class);
		// IF we pass along from the gateway that this is a stock order

		if (gatewayOrder != null) {
			if (gatewayOrder != null && gatewayOrder.getStockOrder()) {
				if (ServiceClass.Standard.equals(context.getServiceClass())) {
					setContextBestEffort(context);
				}
			}
		}

		// IF this is a Standard Order with > 10 line Items
		if (context.getLineItems().size() > 10) {
			if (ServiceClass.Standard.equals(context.getServiceClass())) {
				setContextBestEffort(context);
			}
		}

	}

	private void setContextBestEffort(CoordinatorContext context) {
		log.info(context.getDocumentId() + " is a stock order");
		context.setServiceClass(ServiceClass.BestEffort);
	}

}
