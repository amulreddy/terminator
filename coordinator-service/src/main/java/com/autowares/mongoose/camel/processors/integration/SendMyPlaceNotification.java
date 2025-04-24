package com.autowares.mongoose.camel.processors.integration;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.myplaceintegration.client.MyPlaceDDIClient;
import com.autowares.myplaceintegration.model.MyPlaceNotification;

@Component
public class SendMyPlaceNotification implements Processor {

	private MyPlaceDDIClient myPlaceClient = new MyPlaceDDIClient();

	@Override
	public void process(Exchange exchange) throws Exception {
		MyPlaceNotification notification = exchange.getIn().getBody(MyPlaceNotification.class);
		myPlaceClient.updateMyPlace(notification);
	}

}
