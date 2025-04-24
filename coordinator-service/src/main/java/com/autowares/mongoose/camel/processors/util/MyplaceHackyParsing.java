package com.autowares.mongoose.camel.processors.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.Shop;
import com.autowares.servicescommon.model.OrderSource;
import com.autowares.xmlgateway.model.GatewayOrder;

@Component
public class MyplaceHackyParsing implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {

		CoordinatorOrderContext request = exchange.getIn().getBody(CoordinatorOrderContext.class);
		GatewayOrder orderInput = exchange.getIn().getHeader("gatewayOrder", GatewayOrder.class);

		if (orderInput != null) {
			String iapOrderMessage = orderInput.getIapMessage();
			if (iapOrderMessage != null && OrderSource.MyPlace == request.getOrderSource()) {
				Shop shop = parseIapOrderMessage(iapOrderMessage);
				request.getBusinessContext().setShop(shop);
			}
		}

	}

	public Shop parseIapOrderMessage(String purchaseOrder) {
		
		String regEx = "^Sold To (.*) [a-z]+([0-9]{4}) (.*)";
		Matcher testVar = Pattern.compile(regEx).matcher(purchaseOrder);
		if (testVar.matches()) {
			Shop shop = new Shop();
			shop.setStoreAccountNumber(testVar.group(1));
			shop.setAutowaresAccountNumber(testVar.group(2));
			shop.setBusinessName(testVar.group(3));
			return shop;
		}
		return null;
	}

}
