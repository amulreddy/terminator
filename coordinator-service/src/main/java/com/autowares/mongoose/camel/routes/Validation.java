package com.autowares.mongoose.camel.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class Validation extends RouteBuilder {
    
    public void configure() throws Exception {
        // @formatter:off
        
        onException(Exception.class)
            .handled(true)
            .process("errorNotification");
        
        from("seda:orderValidation")
            .routeId("orderValidation")	
        		.delay(60000).asyncDelayed()
        		.process("validateMoaOrderAgainstProd")
        .end();

    }
}
