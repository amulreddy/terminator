package com.autowares.mongoose.camel.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
public class SupervisoryRoutes extends RouteBuilder {
	
	@Override
	public void configure() throws Exception {
		
		from("quartz://zeroInvoiceProcessing?cron=0/15 * * ? * *")
			.routeId("quartz://zeroInvoiceProcessing")
			.to("direct:zeroinvoice")
		.end();
		
		from("quartz://unprocessedOrders?cron=0/15 * * ? * *")
			.routeId("quartz://unprocessedOrders")	
			.to("direct:unprocessedOrders")
		.end();
		
		from("quartz://unprocessedDocuments?cron=0/15 * * ? * *")
			.routeId("quartz://unprocessedDocuments")	
			.to("direct:unprocessedDocuments")
		.end();

//		from("quartz://sendVisionDocuments?cron=0/90 * * ? * *")
//			.routeId("quartz://sendVisionDocuments")	
//			.to("direct:sendVisionDocuments")
//		.end();
		
	}

}