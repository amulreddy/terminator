package com.autowares.mongoose.camel.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.servicescommon.model.Document;

@Component
public class ErrorRoutes extends RouteBuilder {

	@Override
	public void configure() throws Exception {
		
		/*
		 * IF exceptions happen here, we may want to know about it 
		 */
		
		
		/**
		 * Stop processing
		 */
		from("direct:stopOrderProcessing")
			.routeId("Error: stopOrderProcessing")		
			.process("routeErrorHandler")
			.convertBodyTo(CoordinatorContext.class)
			.process("markContextAsError")     
			.filter(i -> i.getIn().getBody() instanceof CoordinatorOrderContext) 
     			.log("processInViper: : XMLOrderID: ${body.xmlOrderId} ")
				.log("gatewaySetProcessFlag: : XMLOrderID: ${body.xmlOrderId} ")
		    	.process("gatewaySetProcessFlag")
		    .end()
		    .to("direct:stop")
		.end();
		
		from("direct:stop")
			.routeId("Error: stop")		
		    .filter(i -> i.getIn().getBody(Document.class).getDocumentId() != null)
		        .process("markContextAsError") 
		        .process("nonStockFulfillmentErrorHandler")
		        .log("errorNotification: : DocumentID: ${body.documentId} ")
		    	.process("errorNotification")
		    	.log("saveSourceDocument: : DocumentID: ${body.documentId} ")
		    	.process("saveSourceDocument")
		    	.log("documentUnLocker: : DocumentID: ${body.documentId} ")
		    	.process("documentUnLocker")
		    	.log(LoggingLevel.ERROR, "Failed to process: DocumentID: ${body.documentId}")
		    	.log(LoggingLevel.ERROR, "${exception.message}")
		    .end()
		.stop();
		
		from("direct:retrylater")
			.routeId("Error: retrylater")		
	    		.process("documentUnLocker")
	    		.filter(i -> i.getIn().getBody() instanceof Document) 
	    			.log(LoggingLevel.ERROR, "Temporarily failed to process document: ${body.documentId}, retrying later")
	    		.end()
	    		.log(LoggingLevel.ERROR, "${exception.message}")
	    	.end()
	    .stop();
		
	}

}
