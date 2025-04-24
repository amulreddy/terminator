package com.autowares.mongoose.camel.routes;

import com.autowares.xmlgateway.model.GatewayOrder;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.RetryException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

import com.autowares.mongoose.camel.components.OrderContextTypeConverter;
import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.exception.ContinueProcessingException;
import com.autowares.mongoose.exception.LockedDocumentException;
import com.autowares.mongoose.exception.RetryLaterException;
import com.autowares.mongoose.exception.UnimplementedException;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.CoordinatorProcessingPhase;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.servicescommon.exception.ServiceDiscoveryException;
import com.autowares.servicescommon.model.Document;
import com.autowares.servicescommon.model.LocationType;
import com.autowares.servicescommon.model.PurchaseOrderType;
import com.autowares.servicescommon.model.SourceDocumentType;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.supplychain.model.SupplyChainSourceDocument;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

/**
 * Define the application's camel route.
 */
@Component
public class Combo extends RouteBuilder {

	@Value("${spring.profiles.active:local}")
	private String activeProfile;

	@SuppressWarnings("unchecked")
	@Override
	public void configure() throws Exception {
		// @formatter:off
		
		onException(Exception.class)
		  .handled(false)
		  .log("Caught Exception: Exception type.")
		  .to("direct:stopOrderProcessing");
		
		onException(AbortProcessingException.class)
		  .handled(true)
		  .log("Caught Exception: Aborting all processing")
		  .to("direct:stopOrderProcessing");
		
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
		  .delay(5000)
		  .to("direct:retrylater");
		
		from("direct:lockedProcessing")
			.routeId("Combo: lockedProcessing")
			.process("documentLocker")
			.to("direct:phase1")
			.to("direct:phase2")
			.to("direct:phase3")
			.process("documentUnLocker")
		.end();
		
		
		from("direct:gatewayOrderProcessing")
			.routeId("Combo: gatewayOrderProcessing")
			.process("gatewayCheckProcessedStatus")
			.filter(i -> i.getIn().getBody() instanceof CoordinatorOrderContext)
				.filter(i -> !"prod".equals(activeProfile))
		   			.process("writeGatewayOrder")
		   		.end()
		   		.process("myplaceHackyParsing")
		   		.process("purchaseOrderModifier")
		   	.end()
		.end();
		
		/**
		 * Phase 1 - Data lookup
		 */
		from("direct:phase1")
			.routeId("Combo: Data lookup - Phase 1")
			.log(LoggingLevel.WARN, "Phase 1")
			.process(i -> i.getIn().getBody(CoordinatorContext.class).setProcessingPhase(CoordinatorProcessingPhase.One))
			.to("direct:gatewayOrderProcessing")
		    .to("direct:customerLookup") 
			.process("warehouseExclusions")
			.process("customerFiltering")
			.process("nonStockDetection")
			.process("returnDetectionProcessor")
			.process("stockOrderDetection")
			.process("internetSalesSetMustGo")
			.setHeader("replaceSupersededParts", simple("true"))
			.to("direct:lookup")
			.process("dropShipDetectionProcessor")
			.process("lineNumberDedupe")
			.process("fixCustomerPo")
			.choice()
				.when().simple("${body.lineItems.isEmpty()}")
					.throwException(AbortProcessingException.class,"No LineItems to process")
				.when().simple("${body.sourceDocumentType} == ${type:com.autowares.servicescommon.model.SourceDocumentType.Return}")
					.throwException(UnimplementedException.class,"Stop on source document type == Return")
				.when().simple("${body.orderType} == ${type:com.autowares.servicescommon.model.PurchaseOrderType.DropShip}")
				    .to("direct:dropShipProcessing")
				.when().simple("${body.orderType} == ${type:com.autowares.servicescommon.model.PurchaseOrderType.SpecialOrder}")
				    .to("direct:specialOrderProcessing")
				.when().simple("${body.orderType} == ${type:com.autowares.servicescommon.model.PurchaseOrderType.SlotShip}")
					.throwException(UnimplementedException.class,"Stop on source document type == SlotShip")
			 .end()
		.end();

		/**
		 * Phase 2 - Data processing
		 */
		from("direct:phase2")
			.routeId("Combo: Data processing - Phase 2")
			.process(i -> i.getIn().getBody(CoordinatorContext.class).setProcessingPhase(CoordinatorProcessingPhase.Two))
			.process("orderDetailRestriction")
			.process("fulfillmentLocationCreator")
			/* 
			 * Pre Fullfillment planning
			 */
			.split(simple("${body.getFulfillmentSequence}"))
			    .shareUnitOfWork()
				.parallelProcessing()
				.process("partAvailabilityProcessor")
				.log(LoggingLevel.INFO, "Resolving WarehouseMasterData")
				.process("orderWarehouseMasterLookup")
				.choice()
					.when(i -> SystemType.AwiWarehouse.equals(i.getIn().getBody(FulfillmentLocationContext.class).getSystemType()))
						.log("Stocked at Warehouse: ${body.location}")
						.process("logistixShipmentProcessor")
					    .process("truckRunResolvingService")
					    .process("detectShippingDifference")
					.when(i -> SystemType.CounterWorks.equals(i.getIn().getBody(FulfillmentLocationContext.class).getSystemType()))
						.log("Stocked at Counterworks: ${body.location}")
						.process("logistixShipmentProcessor")
					.when(i -> SystemType.MotorState.equals(i.getIn().getBody(FulfillmentLocationContext.class).getSystemType()))
						.log("Stocked at Motorstate: Do not process motorstate orders.")
					.otherwise()
						.log("Potentially Stocked at Vendor: ${body.location}")
					.end()
				.end()
	    	.end()
			
	    	.filter(i ->i.getIn().getHeader("stockedInWarehouse")!=null)
				.log(LoggingLevel.INFO, "Adjusting Quantities for warehouse fulfillment")
				.process("orderQuantityAdjuster")
	    	.end()
	    	
	    	.process("fulfillmentItemPopulator")
	    	
			.filter(i -> i.getIn().getBody(CoordinatorContext.class).getInquiryOptions().getPlanFulfillment())
				.to("direct:fulfillmentPlanning")
			.end()
			
			/*
			 * Post Fullfillment
			 */
			.split(simple("${body.getFulfillmentSequence}"))
				.parallelProcessing()
				.shareUnitOfWork()
				
			    .filter(i -> i.getIn().getBody(FulfillmentLocationContext.class).isBeingFilledFrom())
			   
					.filter(i -> SystemType.CounterWorks.equals(i.getIn().getBody(FulfillmentLocationContext.class).getSystemType()))
						.log("Filling from Counterworks: ${body.location}")
					.end()
					// Filter to only warehouse locations
					.filter(i -> LocationType.Warehouse.equals(i.getIn().getBody(FulfillmentLocationContext.class).getLocationType()))
					    .log(LoggingLevel.INFO, "Resolving TruckRuns For HandlingPriorityType")
					    //.process("truckRunResolvingService")
					    .process("handlingPriorityTypeResolver")
					.end()
				.end()
			.end()
	    		    	
			.filter(i -> i.getIn().getBody(CoordinatorContext.class).getInquiryOptions().getIncludePrices())
				.process("warehouseOrderPricing")
			.end()
			
			.filter(i -> i.getIn().getBody() instanceof CoordinatorOrderContext)
				.log("We have an order")
			.end()
			
			.process("updateWorkingState")

		.end();
		
		/**
		 * Phase 3 - Data persistence
		 */
		from("direct:phase3")
			.routeId("Combo: Data persistence - Phase 3")
			.log("Phase 3")
			.process(i -> i.getIn().getBody(CoordinatorContext.class).setProcessingPhase(CoordinatorProcessingPhase.Three))
			.split(simple("${body.getFulfillmentSequence}"))
				.shareUnitOfWork()
				.parallelProcessing()
				.filter(i -> i.getIn().getBody(FulfillmentLocationContext.class).isBeingFilledFrom())
					.process("logistixShipmentProcessor") // Persist shipment & get tracking number
				.end()
				.filter(i -> SourceDocumentType.Quote.equals(i.getIn().getBody(CoordinatorContext.class).getSourceDocumentType()))
					.log("Quote detected, saving external supplier documents")
					//.to("direct:saveExternalSupplier") // Save supplier documents when we are quoting
				.end()
			.end()
			
//			.filter(i -> OrderSource.MyPlace.equals(i.getIn().getBody(CoordinatorContext.class).getInquiryOptions().getLookupSource()))
//				.to("seda:myPlaceDeliveryIntegration")
//			.end()
			.process("gatewaySetProcessFlag")
			.filter(i -> i.getIn().getBody(CoordinatorContext.class).saveWarehouseOrder())
			    .filter(i -> i.getIn().getBody(CoordinatorOrderContext.class).getInvalid().equals(false))
			        .split(simple("${body.getLineItems}"))
                        .split(simple("${body.getAvailability}"))
                            .filter(i -> i.getIn().getBody(Availability.class).getFillQuantity()>0)
				                .process("orderAllocation")
				            .end()
				        .end()
				    .end()
			     .end()	    
		        //  Saving just the stuff we are not filling.
				.process("saveMoaOrder")
			.end()
			// Save customer order/quote/etc.
			.to("direct:saveCustomerSourceDocument")
			.split(simple("${body.getFulfillmentSequence}"))
		 	    .shareUnitOfWork()
			    .parallelProcessing()
			    // Save off customer packslips
			    .to("seda:saveSupplierSourceDocument")
		    .end()
		    .filter(i -> i.getIn().getBody(CoordinatorContext.class).isManual())
		    	.filter(i -> !i.getIn().getBody(CoordinatorContext.class).isInternal())
		    		.filter(i -> PurchaseOrderType.SpecialOrder.equals(i.getIn().getBody(CoordinatorContext.class).getOrderType()))
		    			.to("direct:customerNotification")
		    		.end()
		    	.end()
		    .end()
		    .filter(i -> i.getIn().getBody() instanceof CoordinatorOrderContext)
		        .to("seda:saveShortage")
		    .end()
		.end();
		
        from("seda:saveShortage")
			.routeId("Combo: saveShortage")
//			.process("deleteShortageDocument")
			.setHeader("__oldbody__", body())
            .process(i -> i.getIn().setBody(OrderContextTypeConverter.coordinatorContextToShortage(i.getIn().getBody(CoordinatorOrderContext.class))))
            .filter(i -> 0 < i.getIn().getBody(SupplyChainSourceDocument.class).getLineItems().size())
            	.log("save Shortage")
            	.process("saveSourceDocument")
            .end()
            .setBody(i->i.getIn().getHeader("__oldbody__"))
        .end();
        
        from("direct:saveExternalSupplier")
				.routeId("Combo: saveExternalSupplier")
			.log("Saving Supplier documents")
			.filter(i -> i.getIn().getBody(CoordinatorContext.class).getProcurementGroupContext() != null)
				.process(i -> i.getIn().setBody(i.getIn().getBody(CoordinatorContext.class).getProcurementGroupContext().getSupplierContext().getOrderContext()))
				.process(i -> i.getIn().getBody(CoordinatorContext.class).setTransactionStage(TransactionStage.Open))
				.process(i -> i.getIn().getBody(CoordinatorContext.class).setTransactionStatus(TransactionStatus.Pending))
				.process("saveSourceDocument")
				.split(simple("${body.getFulfillmentSequence}"))
			 	    .shareUnitOfWork()
				    .parallelProcessing()
				    .process(i -> i.getIn().getBody(FulfillmentLocationContext.class).setTransactionStage(TransactionStage.Ready))
				    // Save off customer packslips
				    .log("saving packslip simple")
				    .process("saveSourceDocument")
			    .end()
			 .end()
        .end();
        
        from("direct:saveExternalSupplierQuote")
				.routeId("Combo: saveExternalSupplierQuote")

			.log("Saving Supplier quote documents")
			.filter(i -> i.getIn().getBody(CoordinatorContext.class).getProcurementGroupContext()!=null)
			    .process(i -> i.getIn().setBody(i.getIn().getBody(CoordinatorContext.class).getProcurementGroupContext().getSupplierContext().getQuoteContext()))
			    .process(i -> i.getIn().getBody(CoordinatorContext.class).setTransactionStage(TransactionStage.Open))
			    .process(i -> i.getIn().getBody(CoordinatorContext.class).setTransactionStatus(TransactionStatus.Pending))
			    .process("saveSourceDocument")
			    .split(simple("${body.getFulfillmentSequence}"))
		 	        .shareUnitOfWork()
			        .parallelProcessing()
			        // Save off supplier packslips
			        .log("saving packslip simple")
			        .process("saveSourceDocument")
		        .end()
		    .end()
        .end();
        
        from("direct:dropShipProcessing")
				.routeId("Combo: dropShipProcessing")
        	.filter(i -> i.getProperty("dropShipAllowed", false, Boolean.class))
     			.log("Allowed Dropship, ")
     			.process("ediLocationCreator")
     			.to("direct:phase2")
     			.filter(i -> i.getIn().getBody() instanceof CoordinatorOrderContext)
     				.to("direct:phase3")
    				.process("generateSupplierPurchaseOrder")
    				.to("direct:saveExternalSupplier")
    			.end()
    			.process("documentUnLocker")
     			.stop()
     		.end()
     	.throwException(UnimplementedException.class,"Stop on source document type == DropShip")
        .end();
        
        from("direct:specialOrderProcessing")
				.routeId("Combo: specialOrderProcessing")
				.process("specialOrderSetup")
//			.filter(i -> i.getProperty("dropShipAllowed", false, Boolean.class))
				.log("Allowed Special Order, ")
//				.process("ediLocationCreator")
				.to("direct:phase2")
				.filter(i -> i.getIn().getBody() instanceof CoordinatorOrderContext)
					.to("direct:phase3")
					.process("generateSupplierPurchaseOrder")
					.to("direct:saveExternalSupplier")
					.process("quoteAcceptanceProcessor")
				.end()
				.process("documentUnLocker")
				.stop()
//			.end()
		.throwException(UnimplementedException.class,"Stop on Special Order.")
		.end();

	}

}
