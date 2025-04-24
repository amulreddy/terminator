package com.autowares.mongoose.camel.processors.postfulfillment;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.camel.components.OrderFillContextTypeConverter;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.servicescommon.client.DiscoverService;
import com.autowares.stockcheck.InquiryType;
import com.autowares.stockcheck.OrderRequest;
import com.counterworks.command.CounterWorksOrderClient;

@Component
@DiscoverService(name = "stores", path = "/stores/order/place/simple")
public class PlaceCounterworksOrder extends CounterWorksOrderClient implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		FulfillmentLocationContext fulfillmentLocationContext = null;

		if (exchange.getIn().getBody() instanceof FulfillmentLocationContext) {
			fulfillmentLocationContext = exchange.getIn().getBody(FulfillmentLocationContext.class);
		} else if (exchange.getIn().getBody() instanceof DocumentContext) {
			fulfillmentLocationContext = exchange.getIn().getBody(DocumentContext.class).getFulfillmentLocationContext();
		}

		OrderRequest request = OrderFillContextTypeConverter
				.fulfillmentLocationContextToCounterWorksOrderRequest(fulfillmentLocationContext);

		request.setCustnum("7");

		placeOrder(fulfillmentLocationContext.getLocation(), request, InquiryType.STORE, true);
	}

}
