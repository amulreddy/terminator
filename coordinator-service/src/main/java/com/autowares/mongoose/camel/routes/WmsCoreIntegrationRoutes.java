package com.autowares.mongoose.camel.routes;

import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.exception.RetryLaterException;
import com.autowares.mongoose.model.IntegrationContext;
import com.autowares.xmlgateway.model.GatewayOrderPlacedEvent;

/**
 * Define the application's camel route.
 */
@Component
public class WmsCoreIntegrationRoutes extends RouteBuilder {

	JacksonDataFormat jsonDataFormat = new JacksonDataFormat(GatewayOrderPlacedEvent.class);

	JacksonDataFormat jsonEventDataFormat = new JacksonDataFormat(Map.class);

	@Override
	public void configure() throws Exception {
		// @formatter:off
		
		/**
		 * Retry in 30 seconds up to 5 times
		 */
		onException(RetryLaterException.class)
			.handled(true);
		
		onException(AbortProcessingException.class)
			.handled(true);
		

		onException(Exception.class)
			.handled(true);
		
		from("{{mongooseWmsEventsQueue}}")
			.routeId("Integration: mongooseWmsEventsQueue")	
			.to("direct:processWmsEvent")
		.end();
		
		from("direct:processWmsEvent")
			.routeId("Integration: processWmsEvent")	
			.doTry()
				.unmarshal(jsonEventDataFormat)
				.endDoTry()
			.doCatch(Exception.class)
					.log("Error parsing json message.")
					.stop()
			.end()
		
			.filter().simple("${body.get(\"eventType\")} == \"gatewayInquiry\" || ${body.get(\"eventType\")} == \"InventoryChange\"")
				.stop()
			.end()

			
			.filter().simple("${body.containsKey(\"eventType\")} && ${body.get(\"eventType\")} == \"OrderUpdate\"")
				.to("direct:orderUpdateRoute")
			.end()
			;
		
		from("direct:orderUpdateRoute")
			.routeId("Integration: orderUpdateRoute")	
			.setHeader("status").simple("${body.get(\"status\")}")
			.setHeader("purchaseOrder").simple("${body.get(\"purchaseOrder\")}")
			.setHeader("shipQuantity").simple("${body.get(\"shipQuantity\")}")
			.setHeader("buildingCode").simple("${body.get(\"buildingCode\")}")
			.setHeader("event").body()
			
			.choice()
			.when().simple("${header.status} == \"Pulled\"")
				.to("seda:operationalEventProcessing")
			.when().simple("${header.status} == \"Packed\"")
				.to("seda:operationalEventProcessing")
			.end()
		.end();
				
		/**
		 * Routes using operational context
		 */
		from("seda:operationalEventProcessing")
			.routeId("Integration: operationalEventProcessing")	
			//.log("Mongoose Operational Event Processing")
			.convertBodyTo(IntegrationContext.class)
			.process("populateOperationalContext")
			.process("integrationContextSummary")
			.process("updatePackslipShipQuantity")
		.end();
		
	}
	
}
