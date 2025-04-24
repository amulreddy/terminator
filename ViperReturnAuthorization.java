package com.autowares.mongoose.camel.processors.integration;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ViperReturnAuthorization implements Processor{
	
	private Logger log = LoggerFactory.getLogger(ViperReturnAuthorization.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		log.debug("In ViperReturnAuthorization");
		
	}

}
