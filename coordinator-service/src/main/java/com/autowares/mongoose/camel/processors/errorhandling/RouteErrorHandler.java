package com.autowares.mongoose.camel.processors.errorhandling;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorContext;

@Component
public class RouteErrorHandler implements Processor {
	
	private static Logger log = LoggerFactory.getLogger(RouteErrorHandler.class);
	
	@Override
	public void process(Exchange exchange) throws Exception {
		 Throwable caused = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
		 CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);
		 log.info("in the error handler");
		 if (context != null) {
		     log.info("we have a context: " + context.getDocumentId());
		     if (caused != null) {
		         log.info("We have a cause: " + context.getDocumentId());
		         context.updateProcessingLog(ExceptionUtils.getRootCauseMessage(caused));
		     }
		 } else {
			 Object o = exchange.getIn().getBody();
			 log.error("We are trying to error handle a context and we have a: " + o.getClass().getSimpleName());
		 }
	}

}
