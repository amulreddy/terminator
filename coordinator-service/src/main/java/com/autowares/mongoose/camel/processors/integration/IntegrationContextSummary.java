package com.autowares.mongoose.camel.processors.integration;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.IntegrationContext;
import com.autowares.mongoose.model.OperationalEventContext;

@Component
public class IntegrationContextSummary implements Processor {

	private static Logger log = LoggerFactory.getLogger(IntegrationContextSummary.class);
	
	@Override
	public void process(Exchange exchange) throws Exception {
		IntegrationContext context = exchange.getIn().getBody(IntegrationContext.class);
		log.debug("Actor: " + context.getActor().getName());
		log.debug("Location: " + context.getLocation().getName());
		log.debug("Timestamp: " + context.getActionTimestamp());
		log.debug("Events: " + context.getOperationalEvents().size());
		log.debug("Originating System: " + context.getOriginatingSystem());
		for(OperationalEventContext event : context.getOperationalEvents()) {
			log.debug("EventType: " + event.getOperationalEventType());
			if(event.getLineItemContext() != null) {
				log.debug("\tProductId: " + event.getLineItemContext().getProductId());
				log.debug("\tQuantity: " + event.getQuantity());
			}
		}
		exchange.getIn().setBody(context);
	}

}
