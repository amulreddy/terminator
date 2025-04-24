package com.autowares.mongoose.camel.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class MotorstateIntegration extends RouteBuilder {

	@Override
	public void configure() throws Exception {
		
		onException(Exception.class)
		  .handled(false)
		  .log("Caught Exception: Exception type.")
		  .process("errorNotification")
		  .to("direct:stop");
		
		from("direct:sendMotorstateLivePO")
			.routeId("sendMotorstateLivePO")
			.process("sendMotorstateLivePO")
			.process("updateWorkingState")
		.end();
	}
	
}
