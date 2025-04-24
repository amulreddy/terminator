package com.autowares.mongoose.camel.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.retry.RetryException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.exception.ContinueProcessingException;
import com.autowares.mongoose.exception.LockedDocumentException;
import com.autowares.mongoose.exception.RetryLaterException;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.servicescommon.exception.ServiceDiscoveryException;
import com.autowares.servicescommon.model.Document;
import com.autowares.servicescommon.model.LocationType;
import com.autowares.servicescommon.model.SystemType;

//import io.github.ncasaux.camelplantuml.routebuilder.CamelPlantUmlRouteBuilder;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

/**
 * Define the application's camel route.
 */
@Component
public class CommonRoutes extends RouteBuilder {

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
		  .log("Caught Retry Later Exception: retrying later")
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
		
		/**
		 * Data lookup
		 */
		from("direct:lookup")
			.routeId("Common: lookup")
			// Next block is processed simultaneously
			.multicast()
				.parallelProcessing()
				.to("direct:partLookupProcessor", "direct:physicalLookup", "direct:findReferenceDocuments")
			.end()
			.multicast()
				.parallelProcessing()
				//   Requires Spatial Data	   Requires Business Data
				.to("direct:storePartLookup", "direct:partyConfigurationLookup")
			.end()
		.end();
		
		from("direct:customerLookup")
			.routeId("Common: customerLookup")
				.log("Looking up customer details")
				.process("orderCustomerLookup")
				.log("Done looking up customer details")
		.end();
		
		from("direct:partLookupProcessor")
			.routeId("Common: partLookupProcessor")	
			.process("orderPartLookup")
		.end();
		
		from("direct:physicalLookup")
			.routeId("Common: physicalLookup")	
			.process("orderPhysicalLocationLookup")
		.end();
		
		from("direct:findReferenceDocuments")
			.routeId("Common: findReferenceDocuments")	
			.process("resolveLineItemDocumentReference") // Same as findQuotesToQuote, should re-inflate context same way
			//.process("rebuildNonstockContext")
			.process("lookupSupplyChainPurchaseOrder")
			.convertBodyTo(CoordinatorContext.class)
		.end();
		
		from("direct:partyConfigurationLookup")
			.routeId("Common: partyConfigurationLookup")	
			.process("buildNonStockContexts")
			.process("partyConfigurationLookup")
		.end();
		
		from("direct:warehouseMasterLookup")
			.routeId("Common: warehouseMasterLookup")		
			.process("orderWarehouseMasterLookup")
		.end();
		
		from("direct:storePartLookup")
			.routeId("Common: storePartLookup")		
			.choice()
				.when().simple("${type:com.autowares.servicescommon.model.LocationType.Store} in ${body.inquiryOptions.locationTypes}")
				.process("storePartLookupProcessor")
			.end()
		.end();
		
		
		from("seda:storePartLookupProcessor")
			.routeId("Common: storePartLookupProcessor")		
			.process("storePartLookupProcessor")
		.end();
		
		/**
		 * Shipment Calculation
		 */
		from("direct:shipmentProcessing")
			.routeId("Common: shipmentProcessing")		
			.split(simple("${body.getFulfillmentSequence}"))
			  .shareUnitOfWork()
				.parallelProcessing()
				.process("logistixShipmentProcessor")
		.end();

		/**
		 * Optaplanner Fulfillment Planning
		 */
		from("direct:fulfillmentPlanning")
			.routeId("Common: fulfillmentPlanning")		
			.split(simple("${body.getLineItems}")) // Split on Fulfillment Items
				.shareUnitOfWork()
				.parallelProcessing()
				.process("optaplannerFulfillmentPlanning")
		.end();
		
		
		from("direct:toExternalSupplierOrder")
			.routeId("Common: toExternalSupplierOrder")		
			.log("In External Supplier Order block")
			.log("SystemType for Fullfillment Location: ${body.location} SystemType: ${body.systemType} LocationType: ${body.locationType}")
			.filter(i -> LocationType.Vendor.equals(i.getIn().getBody(FulfillmentLocationContext.class).getLocationType()))
				.filter(i -> i.getIn().getBody(FulfillmentLocationContext.class).isQuoted() 
						&& i.getIn().getBody(FulfillmentLocationContext.class).isOrder())
				    .log("This order is quoted")
					.choice()
					    .when(i -> SystemType.VIC.equals(i.getIn().getBody(FulfillmentLocationContext.class).getSystemType()))
					        .log("Calling VIC")
					        .process("vicProcessor")
					    .when(i -> SystemType.MotorState.equals(i.getIn().getBody(FulfillmentLocationContext.class).getSystemType()))
	//			    	     .process("motorstateProcessor")
					         //  Send to Greg's future Motorstate specific processor.
					         .log("In MOTORSTATE block")
					    .when(i -> SystemType.Manual.equals(i.getIn().getBody(FulfillmentLocationContext.class).getSystemType()))
						        .log("Manual")
						        
						.otherwise()
						    .log("Unhandled SystemType for Fullfillment Location: ${body.location} SystemType: ${body.systemType}")
					.end()
				.end()
			.end()
		.end();
		
		from("seda:saveSupplierSourceDocument")
		.routeId("Common: saveSupplierSourceDocument")		
			.choice()
				.when(i -> (i.getIn().getBody(FulfillmentLocationContext.class).isOrder() && 
						!i.getIn().getBody(FulfillmentLocationContext.class).isBeingFilledFrom()))
					//  No op.   Do not save source docs in this case.
				.when(i -> (!i.getIn().getBody(FulfillmentLocationContext.class).isOrder() && 
						SystemType.AwiWarehouse.equals(i.getIn().getBody(FulfillmentLocationContext.class).getSystemType())))
					//  No op. Do not quote Warehouse fulfillment at this time
//				.when(i -> (i.getIn().getBody(FulfillmentLocationContext.class).isOrder() && 
//				        i.getIn().getBody(FulfillmentLocationContext.class).getNonStockContext().hasThirdPartyFulfullment()))
//					.to("seda:nonStockPersistence")
//					.log("Saving NonStock Customer Packslip")
//					.process("saveSourceDocument")
				.when(i -> (i.getIn().getBody(FulfillmentLocationContext.class).isOrder() && 
						LocationType.Vendor.equals(i.getIn().getBody(FulfillmentLocationContext.class).getLocationType())) && 
						!SystemType.MotorState.equals(i.getIn().getBody(FulfillmentLocationContext.class).getSystemType()))
					// 	Strip out saving customer packslip for 3rd party fulfillment
				.otherwise()
					.log("Saving Customer Packslip")
					.process("saveSourceDocument")
		.end();
			
//		from("seda:nonStockPersistence")
//			.setBody(i -> i.getIn().getBody(FulfillmentLocationContext.class).getNonStockContext())
//			.split(simple("${body.getFulfillmentSequence}"))
//				.shareUnitOfWork()
//				.log("Would want to save a Supplier Packslip")
//				.process("saveSourceDocument")
//			.end()
//			.log("Would want to save a Supplier Order")
//			.process("saveSourceDocument")
//		.end();
		
		from("direct:saveCustomerSourceDocument")
			.routeId("Common: saveCustomerSourceDocument")		
			.log("Saving Customer Document")
			.process("saveSourceDocument");
		
		from("direct:customerNotification")
			.routeId("Common: customerNotification")		
			.log(LoggingLevel.INFO, "Notifiy customer: ${body.documentId}")
			.process("orderCustomerLookup")
			.process("customerNotification")
    .end();

		// Add route to be able to generate PlantUML
//		getContext().addRoutes(new CamelPlantUmlRouteBuilder("localhost", 8090));

						
	}

}
