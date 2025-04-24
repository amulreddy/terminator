package com.autowares.mongoose.camel.processors.util;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.xmlgateway.model.GatewayOrder;

@Component
public class PurchaseOrderModifier implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {

		CoordinatorOrderContext request = exchange.getIn().getBody(CoordinatorOrderContext.class);
		GatewayOrder orderInput = exchange.getIn().getHeader("gatewayOrder", GatewayOrder.class);

		if (orderInput != null) {
			String purchaseOrder = orderInput.getPurchaseOrder();
			String orderMsg = orderInput.getOrderMessage();
			if (orderMsg != null && !"null".equalsIgnoreCase(orderMsg) && !"stock order".equalsIgnoreCase(orderMsg)) {
				purchaseOrder = purchaseOrder + ' ' + orderMsg;
				purchaseOrder = StringUtils.truncate(purchaseOrder, 20);
				request.setPurchaseOrder(purchaseOrder);
			}
		}

	}

}
