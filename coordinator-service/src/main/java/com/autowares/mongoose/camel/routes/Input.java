package com.autowares.mongoose.camel.routes;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.spi.ThreadPoolProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.exception.ContinueProcessingException;
import com.autowares.mongoose.exception.LockedDocumentException;
import com.autowares.mongoose.exception.RetryLaterException;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.servicescommon.exception.ServiceDiscoveryException;
import com.autowares.servicescommon.model.Document;
import com.autowares.servicescommon.model.SourceDocument;
import com.autowares.servicescommon.model.SourceDocumentType;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.servicescommon.model.WorkingDocument;
import com.autowares.servicescommon.util.SequenceGenerator;
import com.autowares.supplychain.model.SupplyChainSourceDocument;
import com.autowares.supplychain.model.TransactionScope;
import com.autowares.xmlgateway.edi.base.EdiDocument;
import com.autowares.xmlgateway.model.GatewayOrderPlacedEvent;
import com.autowares.xmlgateway.model.OrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class Input extends RouteBuilder {

	@Autowired
	private ObjectMapper objectMapper;

	OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();
	JacksonDataFormat jsonDataFormat;

	@SuppressWarnings("unchecked")
	@Override
	public void configure() throws Exception {
		ThreadPoolProfile poolProfile = new ThreadPoolProfile("masterPoolProfile");
		poolProfile.setMaxPoolSize(mxBean.getAvailableProcessors());
		poolProfile.setMaxQueueSize(100);
		poolProfile.setPoolSize(mxBean.getAvailableProcessors());
		poolProfile.setKeepAliveTime(1L);
		poolProfile.setId(UUID.randomUUID().toString());
		poolProfile.setTimeUnit(TimeUnit.MINUTES);
		getContext().getExecutorServiceManager().setDefaultThreadPoolProfile(poolProfile);
		jsonDataFormat =  new JacksonDataFormat(objectMapper, GatewayOrderPlacedEvent.class);
		

		onException(LockedDocumentException.class)
		  .handled(true)
		  .filter(i -> i.getIn().getBody() instanceof Document)
		  	.log("Caught Locked Document ${body.documentId}")
		  .end()
		  .filter(i -> !(i.getIn().getBody() instanceof Document))
		  	.log("Caught Locked Document Exception: Stop Processing")
		  .end()
		  .stop();
		
		onException(RetryLaterException.class)
		  .log("Caught Retry Exception: retrying later")
		  .to("direct:retrylater")
		  .handled(true);
		
		
		onException(ContinueProcessingException.class)
		  .log("Caught Handled Exception: Continuing")
		  .process("routeErrorHandler")
		  .continued(true);
		
		onException(ServiceDiscoveryException.class, HttpServerErrorException.class)
		  .maximumRedeliveries(3)
		  .log("Caught Http Server Exception: Retrying")
		  .handled(false)
		  .delay(1000);
		
		onException(AbortProcessingException.class)
		  .handled(true)
		  .log("Caught Exception: Aborting all processing")
		  .to("direct:stopOrderProcessing");
		
		// @formatter:off
		
		/**
		 * New production orders coming in from the gateway via events
		 */
		from("{{gatewayOrdersQueue}}")
			.routeId("Input: gatewayOrdersQueue")
		    .setHeader("activeMqSource", simple("Prod"))
		    .to("diret:eventProcessing")
		.end();
		
		/**
		 * New test orders coming in from the gateway via events
		 */
		from("{{testGatewayOrdersQueue}}")
			.routeId("Input: testGatewayOrdersQueue")
			.setHeader("activeMqSource", simple("Test"))
			.to("direct:eventProcessing")
		.end();
		
		from("direct:eventProcessing") 
			.routeId("Input: eventProcessing")
			.unmarshal(jsonDataFormat)
			.setHeader("gatewayOrder", simple("${body.order}"))
			.filter().simple("${body.eventType} == \"gatewayOrder\"")
				.choice()
					.when(i -> i.getIn().getBody(GatewayOrderPlacedEvent.class).getOrder().getRequest() != null) 
					   .setBody(i -> i.getIn().getBody(GatewayOrderPlacedEvent.class).getOrder().getRequest())
					       .log(LoggingLevel.INFO, "${header.activeMqSource} orderRequest: Document ID: ${body.documentId} ACCT: ${body.accountNumber}")
					       .convertBodyTo(CoordinatorOrderContext.class)
					   .otherwise()
					       .log(LoggingLevel.INFO, "${header.activeMqSource} order: ID: ${body.orderId} ACCT: ${body.order.gatewayRequest.autowaresAccountNumber} SOURCE: ${body.order.orderSource} METHOD: ${body.order.deliveryMethod} CLASS: ${body.order.serviceClass} SIZE: ${body.order.gatewayRequest.getParts.size()}")
					       .convertBodyTo(CoordinatorOrderContext.class)
				.end()
				.to("direct:lockedProcessing")
			.end()
		.end();
		
		/**
		 * Unprocessed orders from the gateway
		 */
		from("direct:unprocessedOrders")
			.routeId("Input: unprocessedOrders")
			.process("gatewayFindUnprocessedOrders")
			.split(body())
				.parallelProcessing()
				.log(LoggingLevel.INFO, "Unprocessed order: ID: ${body.order.xmlOrderId} ACCT: ${body.order.gatewayRequest.autowaresAccountNumber} SOURCE: ${body.order.orderSource} METHOD: ${body.order.deliveryMethod} CLASS: ${body.order.serviceClass} SIZE: ${body.order.gatewayRequest.getParts.size()}")
				.setHeader("gatewayOrder", simple("${body.order}"))
				.convertBodyTo(CoordinatorOrderContext.class)
				.to("direct:lockedProcessing")
		.end();
		
		/**
		 * Zeroed orders from MOA Orders
		 */
		from("direct:zeroinvoice")
			.routeId("Input: zeroinvoice")
			.process("findZeroShipOrders")
			.split().body()
				.log(LoggingLevel.INFO, "Zero Ship Order: ${body.documentId}")
				.process("documentLocker")
				.process("orderCustomerLookup")
	        	.process("orderPartLookup")
	        	.process("warehouseOrderPricing")
				.split(simple("${body.getFulfillmentSequence}"))
				    .process("zeroShipmentProcessor")
				    .process("moaOrderToInvoiceProcessor")
				.end()
				.process("documentUnLocker")
		.end();
		
		/**
		 * Unprocessed documents
		 */
		from("direct:unprocessedDocuments")
			.routeId("Input: unprocessedDocuments")
			.process("findDocumentsToProcess")
			.split().body().parallelProcessing()
				.log(LoggingLevel.INFO, "Found Document To Process: ${body.documentId} ${body.sourceDocumentType}")
				  .choice()
				    .when(d -> SourceDocumentType.Quote.equals(d.getIn().getBody(SourceDocument.class).getSourceDocumentType()))
				        .choice()
				            .when(q -> TransactionStatus.Pending.equals(q.getIn().getBody(WorkingDocument.class).getTransactionStatus()))
				                .to("direct:tempQuotePendingBlock")
				        .otherwise()
				            .to("direct:processQuote")
				        .endChoice()
				    .when(d -> SourceDocumentType.ShippingEstimate.equals(d.getIn().getBody(SourceDocument.class).getSourceDocumentType()))
//				        .process("documentLocker")
				        .convertBodyTo(DocumentContext.class)
				        .process("lookupSupplyChainPurchaseOrder")
				        .to("direct:processShippingEstimate")
					.when(d -> SourceDocumentType.PurchaseOrder.equals(d.getIn().getBody(SourceDocument.class).getSourceDocumentType()))
						.choice()
							.when(d -> isPurchasingPurchaseOrder(d))
								.log("We have a purchasing purchase order.")
								.process("documentLocker")
								.to("direct:yoozFtp")
								.process("updateWorkingState")
								.process("saveSourceDocument")
								.process("documentUnLocker")
						.otherwise()
				        	.convertBodyTo(CoordinatorOrderContext.class)
				        	.to("direct:lockedProcessing")
				        .endChoice()
				    .when(d -> SourceDocumentType.PackSlip.equals(d.getIn().getBody(SourceDocument.class).getSourceDocumentType()))
				        .to("direct:packslipProcessing")
				.end()
			.end()
		.end();
		
		/**
		 * Directly placed orders through the API
		 */
		from("direct:api")
			.routeId("Input: api")
			.log(LoggingLevel.INFO, "New order from API")
			.convertBodyTo(CoordinatorOrderContext.class)
				.to("direct:phase1")
				.to("direct:phase2")
				.to("direct:phase3")
			.end()
		.end();

		/**
		 * Directly placed quote through the API, processed inline
		 */
		from("direct:processQuote")
			.routeId("Input: processQuote")
			.log(LoggingLevel.INFO, "New quote to process")
			.convertBodyTo(CoordinatorContext.class)
			.process("documentLocker")
			.process("detectProcessedState")
			.to("direct:phase1")
			.process("warehouseOrderPricing")
			.log(LoggingLevel.INFO, "Notifying Customer Service through Manual workflow")
//	 		.process(i -> i.getIn().getBody(CoordinatorContext.class).setTransactionStage(TransactionStage.Open))
//			.process(i -> i.getIn().getBody(CoordinatorContext.class).setTransactionStatus(TransactionStatus.Processing))
//			.process(i -> i.getIn().getBody(CoordinatorContextImpl.class).setOrderType(PurchaseOrderType.SpecialOrder))
			.split(simple("${body.getFulfillmentSequence}"))
				.choice()
					.when(d -> SystemType.VIC.equals(d.getIn().getBody(FulfillmentLocationContext.class).getSystemType()))
						.process("vicIpoProcessor")
			            .process("flatRateShipping")
					.when(d -> SystemType.Manual.equals(d.getIn().getBody(FulfillmentLocationContext.class).getSystemType()))
						.log("Manual system type")
						.process("manualQuoteProcessor")
					.otherwise()
						.log("Unsupported system type.")
						.process("manualQuoteProcessor")
				.end()
			.end()
		    .filter(i -> TransactionStatus.Error.equals(i.getIn().getBody(CoordinatorContext.class).getTransactionStatus()))
		    	.throwException(new AbortProcessingException())
		    .end()
		    .process(i -> i.getIn().getBody(CoordinatorContext.class).setTransactionStage(TransactionStage.Open))
			.process("saveSourceDocument")
			.to("direct:saveExternalSupplierQuote")
			.process("documentUnLocker")
		.end();
		
		from("direct:tempQuotePendingBlock")
			.routeId("Input: tempQuotePendingBlock")
		    .convertBodyTo(CoordinatorContext.class)
		    .process("documentLocker")
		    .process("detectProcessedState")
		    .to("direct:phase1")
		    .log(LoggingLevel.INFO, "Notifying customer through Manual Pending workflow")
		    .process(i -> i.getIn().getBody(CoordinatorContext.class).setTransactionStage(TransactionStage.Open))
		    .process(i -> i.getIn().getBody(CoordinatorContext.class).setTransactionStatus(TransactionStatus.Pending))
            .process("saveSourceDocument")
            .process("documentUnLocker")
		.end();
		
		from("direct:tempSaveSupplierDocument")
			.routeId("Input: tempSaveSupplierDocument")
		.split(simple("${body.getFulfillmentSequence}"))
		    .shareUnitOfWork()
		    .parallelProcessing()
		    .to("seda:saveSupplierSourceDocument")
        .end();
		
		/**
		 * Direct inquire through the API
		 */
		from("direct:inquiry")
			.routeId("Input: inquiry")
			.log(LoggingLevel.WARN, "Inquiry from API")
			.convertBodyTo(CoordinatorContext.class)
				.to("direct:phase1")
				.to("direct:phase2")
		.end();
		
		/**
		 * Direct quote through the API, persist to process async
		 */
		from("direct:saveInitialQuote")
			.routeId("Input: saveInitialQuote")
			.log(LoggingLevel.WARN, "Initial Quote only.")
			.convertBodyTo(CoordinatorContext.class)
			.process(i -> i.getIn().getBody(CoordinatorContext.class).setTransactionStage(TransactionStage.Ready))
			.process("orderCustomerLookup")
			.process("saveSourceDocument")
		.end();
		
		from("direct:cancel")
			.routeId("Input: cancel")
			.log(LoggingLevel.INFO, "Cancelling.")
			.process("lookupSupplyChainPurchaseOrder")
			.process("cancelRequestProcessor")
		.end();
		
		from("direct:generate-invoice")
			.routeId("Input: generate-invoice")
			.log(LoggingLevel.INFO, "Invoicing.")
			.process("lookupSupplyChainPurchaseOrder")
			.process("invoiceRequestProcessor")
			.process("orderCustomerLookup")
			.process("orderPartLookup")
			.process("moaOrderToInvoiceProcessor")
		.end();
		
		from("direct:ediInput")
			.routeId("Input: ediInput")
		    .process(i -> i.getIn().getBody(OrderRequest.class).setDocumentId(String.valueOf(new SequenceGenerator().nextId())))
		    .convertBodyTo(CoordinatorOrderContext.class)
		    .to("direct:phase1")
		    .process("warehouseOrderPricing")
 		    .process("productToSupplier")
 		    .convertBodyTo(EdiDocument.class)
 		    .setBody(i -> i.getIn().getBody(EdiDocument.class).toString())
 		    .log("${body}")
 		    //TODO filename?
 		    .to("direct:ediFtp")
		.end();
		
        from("direct:basicRouteTest")
        	.routeId("basicRouteTest")
        	.setBody(constant("Hello Java Buddies!"))
        	.to("file:output")
        .end();
        
//        from("direct:sendVisionDocuments")
//            .routeId("sendVisionDocuments")
//            .process("processVisionDocuments")
//        .end();
		
	}
	
	private Boolean isPurchasingPurchaseOrder(Exchange i) {
		if(i != null && i.getIn() != null && i.getIn().getBody() instanceof SupplyChainSourceDocument) {
			SupplyChainSourceDocument sourceDocument = i.getIn().getBody(SupplyChainSourceDocument.class);
			if (sourceDocument.getTransactionContext() != null) {
				return TransactionScope.Purchasing.equals(sourceDocument.getTransactionContext().getTransactionScope());
			}
		}

		if(i != null && i.getIn() != null && i.getIn().getBody() instanceof DocumentContext) {
			DocumentContext documentContext = i.getIn().getBody(DocumentContext.class);
			if(documentContext.getTransactionalContext() != null) {
				return TransactionScope.Purchasing.equals(documentContext.getTransactionalContext().getTransactionScope());
			}
		}
		return false;
	}

}
