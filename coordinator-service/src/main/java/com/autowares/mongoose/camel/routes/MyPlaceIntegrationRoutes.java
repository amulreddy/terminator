package com.autowares.mongoose.camel.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.myplaceintegration.model.MyPlaceBaseNotification;
import com.autowares.myplaceintegration.model.MyPlaceOrderNotification;

@Component
public class MyPlaceIntegrationRoutes extends RouteBuilder {

	@Override
	public void configure() throws Exception {
		// @formatter:off
		
		from("seda:myPlaceDeliveryIntegration")
			.routeId("myPlaceDeliveryIntegration")
			.choice()
				.when().simple("${body} is 'java.util.Map'")
//					.log("got DTIAPI event")
					.convertBodyTo(MyPlaceBaseNotification.class)
					.to("seda:UpdateMyPlace")
				.when().simple("${body} is 'com.autowares.mongoose.model.CoordinatorOrderContext'")
					//Filter to 542 Maxi Bay City.
					.filter(i -> Long.valueOf(542).equals(i.getIn().getBody(CoordinatorOrderContext.class).getCustomerNumber()))
						.split(simple("${body.getFulfillmentSequence}"))
						.filter(i -> i.getIn().getBody(FulfillmentLocationContext.class).isBeingFilledFrom())
//							.log("got order")
							.convertBodyTo(MyPlaceOrderNotification.class)
							.to("seda:UpdateMyPlace")
						.end()
					.end()
			.end()
			
		.end();
	    	
		from("seda:UpdateMyPlace")
			.routeId("UpdateMyPlace")
			//.log(LoggingLevel.DEBUG, "updateMyPlace")
//			.process("prettyPrinter")
			.filter(i -> "542".equals(i.getIn().getBody(MyPlaceOrderNotification.class).getSellFromId()))
				.process("sendMyPlaceNotification")
			.end()
		.end();
		
	}
}
