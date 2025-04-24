package com.autowares.mongoose.camel.routes;

import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.stereotype.Component;

import com.autowares.events.AwiEvent;

/**
 * Define the application's camel route.
 */
@Component
public class DashIntegrationRoutes extends RouteBuilder {

	JacksonDataFormat jsonDataFormat = new JacksonDataFormat(AwiEvent.class);

	JacksonDataFormat jsonEventDataFormat = new JacksonDataFormat(Map.class);

	@Override
	public void configure() throws Exception {
		// @formatter:off
		

		from("{{mongooseCoordinatorDashQueue}}")
		.routeId("Dash: mongooseCoordinatorDashQueue")		
			.doTry()
				.unmarshal(jsonEventDataFormat)
				.endDoTry()
			.doCatch(Exception.class)
					.log("Error parsing json message.")
					.stop()
			.end()
			
			.filter().simple("${body.containsKey(\"eventType\")} && ${body.get(\"eventType\")} == \"DashItemScanned\"")
				.filter().simple("${body.containsKey(\"scanType\")} && ${body.get(\"scanType\")} == \"pickup\"")
					.process("toteAccounting")
				.end()
			.end()
			
			.filter().simple("${body.containsKey(\"eventType\")} && ${body.get(\"eventType\")} == \"DashCwOrderEvent\"")
					.to("seda:myPlaceDeliveryIntegration")
			.end()
			
	    .end();
		
	}
}
