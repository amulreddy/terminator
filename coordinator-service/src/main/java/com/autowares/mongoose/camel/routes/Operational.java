package com.autowares.mongoose.camel.routes;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.RetryException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.exception.ContinueProcessingException;
import com.autowares.mongoose.exception.LockedDocumentException;
import com.autowares.mongoose.exception.RetryLaterException;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.servicescommon.exception.ServiceDiscoveryException;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.servicescommon.util.CronSchedule;
import com.google.common.collect.Lists;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

@Component
public class Operational extends RouteBuilder {

	@Value("${spring.profiles.active:local}")
	private String activeProfile;

	private static final List<String> motorstateProcessingSchedules = Lists.newArrayList("* 0-30 08-17 ? * 02-05",
			"* * 08-17 ? * 06");

	@SuppressWarnings("unchecked")
	public void configure() throws Exception {
		// @formatter:off
        
        onException(Exception.class)
            .handled(false)
            .log("Caught Exception: Exception type.")
            .filter(i -> i.getIn().getBody() instanceof DocumentContext)
            	.setBody(i -> i.getIn().getBody(DocumentContext.class).getSourceDocument())
            .end()
            .process("markContextAsError") 
            .process("errorNotification")
            .to("direct:retryLater");
        
        onException(AbortProcessingException.class)
		  .handled(true)
		  .log("Caught Exception: Aborting all processing")
		  .process(i -> { if(i.getException() != null) { i.getException().printStackTrace(); } })
		  .to("direct:stop");
        
        onException(LockedDocumentException.class)
		  .handled(true)
//		  .filter(i -> i.getIn().getBody() instanceof Document)
//		  	.log("Caught Locked Document ${body.documentId}")
//		  .end()
//		  .filter(i -> !(i.getIn().getBody() instanceof Document))
//		  	.log("Caught Locked Document Exception: Stop Processing")
//		  .end()
		  .stop();
          
        onException(RetryLaterException.class)
            .handled(true)
            .log("Caught Retry Exception: retrying later")
            .to("direct:retrylater");
          
        onException(ContinueProcessingException.class)
            .continued(true)
            .log("Caught Handled Exception: Continuing")
            .process("routeErrorHandler");
          
        onException(ServiceDiscoveryException.class, HttpServerErrorException.class, RetryException.class, CallNotPermittedException.class)
            .handled(false)
            .maximumRedeliveries(3)
            .log("Exhausted efforts retrying, giving up for now")
            .delay(3000)
            .to("direct:retrylater");
      
        from("direct:packslipProcessing")
			.routeId("Operational: packslipProcessing")
			.process("documentLocker")
            .process("lookupSupplyChainPurchaseOrder")
            .process("detectProcessedState")
            .process("orderCustomerLookup")
        	.process("orderPartLookup")
			.process("partAvailabilityProcessor")
            .process("logistixShipmentProcessor")
            .process("internetSalesSetMustGo")
            .filter(i -> i.getIn().getBody(DocumentContext.class).getFulfillmentLocationContext() != null)
                .to("direct:packslipFulfillmentProcessing")
            .end()
            .process("documentUnLocker")
        .end();
            
        from("direct:packslipFulfillmentProcessing")
        	.routeId("Operational:packslipFulfillmentProcessing")
        	.choice()
			    .when(i -> fulfillmentLocationContextSystemTypeIs(i, SystemType.AwiWarehouse))
	                .log("Saving Warehouse Order: ${body.fulfillmentLocationContext.location}")
	                .to("direct:warehouseOrderProcessing")
	                // Persistence at the Coordinator context / Fulfillment Context levels
	                .log("Persisting to warehouses")
	                .to("direct:warehousePersistence")
	            .when(i -> fulfillmentLocationContextSystemTypeIs(i, SystemType.CounterWorks))
	        	    .log("Saving Counterworks Order: ${body.fulfillmentLocationContext.location}")
	        	    .process("placeCounterworksOrder")
	        	.when(i -> fulfillmentLocationContextSystemTypeIs(i, SystemType.GCommerceEDI))
	    		    .log("Processing GCommerce records.")
	 		        .process("warehouseOrderPricing")
	 		        .process("productToSupplier")
	 		        .to("direct:ediFtp")
	 		        .to("direct:ediSPSCommerce")
	 		    .when(i -> fulfillmentLocationContextSystemTypeIs(i, SystemType.Manual))
	       	        .log("Processing manual order.")
				    .to("direct:customerNotification")
	        	.when(i -> fulfillmentLocationContextSystemTypeIs(i, SystemType.MotorState))
	        	    .log("Processing Motorstate records.")
	        	    .to("direct:motorstateProcessing")
        	    .when(i -> fulfillmentLocationContextSystemTypeIs(i, SystemType.VIC))
	        	    .log("Processing VIC records.")
	        	    .to("direct:vicOrderProcessing")
	        .end()
	        .choice()
	        	.when(i -> sourceDocumentSystemTypeIs(i, SystemType.GCommerceEDI) || sourceDocumentSystemTypeIs(i, SystemType.VIC))
	        		// Instead of saving the packslip, we remove it.
	        		// We will get ASN's in through GCommerce from the supplier later which will create new packslip documents
	        		.log("Clean up packslip.")
	        		.process("cleanUpPlannedPackslip")
	        	.otherwise()
	        		.process("updateWorkingState")
	        		.process("saveSourceDocument")
        	.end()
        .end();
        
        from("direct:processShippingEstimate")
            .routeId("Operational:processShippingEstimate")
            .log("Shipment Estimate Processing")
            .process("vicIpoProcessor")
            .process("flatRateShipping")
            .process("updateWorkingState")
    		.process("saveSourceDocument")
        .end();
        
	    from("direct:warehouseOrderProcessing")
			.routeId("Operational: warehouseOrderProcessing")
			.log(LoggingLevel.INFO, "Resolving WarehouseMasterData")
			.process("orderWarehouseMasterLookup")
			.log(LoggingLevel.INFO, "Resolving TruckRuns")
			.process("truckRunResolvingService")
			.log(LoggingLevel.INFO, "Determining order handling")
			.process("orderHandlingProcessor")
			.process("handlingPriorityTypeResolver")
		.end();
	
		from("direct:warehousePersistence")
			.routeId("Operational: warehousePersistence")
			.process("saveMoaOrder")
			.filter(i -> "test".equals(activeProfile))
				.to("seda:orderValidation")
			.end()
			.log("Persisted to warehouse")
		.end();
		
		from("direct:motorstateProcessing")
			.routeId("Operational: motorstateProcessing")
			.filter(i -> "prod".equals(activeProfile))
	        	.filter(i -> !motorstateProcessingSchedules.stream().anyMatch(s -> CronSchedule.isCurrentlyActive(s)))
			    	.throwException(RetryLaterException.class, "Not in Motorstate processing window")
			    .end()
			    .to("direct:warehouseOrderProcessing")
			    .process("lookUpMotorstatePart")
			    .process("sendMotorstateValidatePO")
			    .to("direct:sendMotorstateLivePO")
			    // Persistence at the Coordinator context / Fulfillment Context levels
			    .log("Persisting to warehouses")
			    .to("direct:warehousePersistence")
			    .log("Saving Motorstate invoices.")
//			    .process("moaOrderToInvoiceProcessor")
			.end()
		.end();
		
		from("direct:vicOrderProcessing")
			.routeId("Operational: vicOrderProcessing")
			.process("vicOrderProcessor")
		.end();

    }

	private Boolean fulfillmentLocationContextSystemTypeIs(Exchange i, SystemType systemType) {
		if(i != null && i.getIn() != null && i.getIn().getBody() instanceof DocumentContext) {
			DocumentContext documentContext = i.getIn().getBody(DocumentContext.class);
			if(documentContext.getFulfillmentLocationContext() != null) {
				return systemType.equals(documentContext.getFulfillmentLocationContext().getSystemType());
			}
		}
		return false;
	}
	
	private Boolean sourceDocumentSystemTypeIs(Exchange i, SystemType systemType) {
		if(i != null && i.getIn() != null && i.getIn().getBody() instanceof DocumentContext) {
			DocumentContext documentContext = i.getIn().getBody(DocumentContext.class);
			if(documentContext.getSourceDocument() != null) {
				return systemType.equals(documentContext.getSourceDocument().getSystemType());
			}
		}
		return false;
	}
}
