package com.autowares.mongoose.camel.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.exception.LockedDocumentException;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.service.LockedDocuments;
import com.autowares.servicescommon.model.Document;
import com.autowares.servicescommon.model.SourceDocumentType;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.xmlgateway.edi.EdiSourceDocument;
import com.autowares.xmlgateway.edi.base.EdiDocument;

@Component
@Profile("!unitTest")
public class FtpRoutes  extends RouteBuilder  {
	
	@Value("${spring.profiles.active:local}")
	private String activeProfile;
	
	@Autowired
	private LockedDocuments lockedDocuments;

    @Override
    public void configure() throws Exception {
    	
        onException(AbortProcessingException.class)
		  .handled(true)
		  .log(LoggingLevel.ERROR, "Caught Exception: ${exception.message}")
		  .process("documentUnLocker")
		  .stop();
        
		onException(LockedDocumentException.class)
		  .handled(true)
		  .filter(i -> i.getIn().getBody() instanceof Document)
		  	.log("Caught Locked Document ${body.documentId}")
		  .end()
		  .filter(i -> !(i.getIn().getBody() instanceof Document))
		  	.log("Caught Locked Document Exception: Stop Processing")
		  .end()
		  .stop();
		
        
        onException(Exception.class)
		  .handled(false)
		  .log(LoggingLevel.ERROR, "Caught Unknown Exception: ${exception.message}")
		  .process("documentUnLocker")
		  .stop();
        
    	
        from("ftp://viper.autowares.com/../../usr1/datafiles?username=jenkins&password=feiSh4&noop=true&include=^VC_PK.*&passiveMode=true")
        	.routeId("Viper PK Files")
        	.log(LoggingLevel.DEBUG, "${header.CamelFileNameConsumed}")
        	.log(LoggingLevel.DEBUG, "${body}")
        	.process("pickupWarehouseVendorIntegration")
        	.to("file://out")
        .end();        
        
        from("{{ediDocumentSource}}")
            .idempotentConsumer(header("CamelFileNameConsumed"),lockedDocuments)
        	.routeId("Viper GCom Files")
//        	.filter(i -> !"prod".equals(activeProfile))
        	.log(LoggingLevel.INFO, "${header.CamelFileNameConsumed}")
        	.convertBodyTo(String.class)
        	.convertBodyTo(EdiDocument.class)
        	.filter(i -> i.getIn().getHeader("CamelFileNameConsumed", String.class).startsWith("997"))
        	    .log("Received 997 document")
        	    .process("edi997PurchaseOrderResolver")
				.filter().simple("${body} is 'com.autowares.mongoose.model.CoordinatorOrderContext'")
					.filter(i -> !TransactionStatus.Accepted.equals(i.getIn().getBody(CoordinatorContext.class).getTransactionStatus()))
						.log("Processing 997 doc.")
						.process(i -> i.getIn().getBody(CoordinatorContext.class).setTransactionStatus(TransactionStatus.Accepted))
						.process(i -> i.getIn().getBody(CoordinatorContext.class).setTransactionStage(TransactionStage.Open))
						.process("saveSourceDocument")
						.setHeader("notificationSubject",simple("Order acknowledged"))
						.process("customerNotification")
					.end()
				.end()
        	    .stop()
        	.end()    
        	.process(i -> i.getIn().setBody(EdiSourceDocument.builder().fromEdiDocument(i.getIn().getBody(EdiDocument.class)).buildSourceDocuments()))
        	.split(body())
        		.process(i -> i.getIn().getBody(EdiSourceDocument.class).setSystemType(SystemType.GCommerceEDI))
        		.filter(i -> SourceDocumentType.PurchaseOrder.equals(i.getIn().getBody(EdiSourceDocument.class).getSourceDocumentType()))
					.log("PO Processing ${body.documentId}")
					.stop()
				.end()	
				.filter(i -> SourceDocumentType.PackSlip.equals(i.getIn().getBody(EdiSourceDocument.class).getSourceDocumentType()))
					.log("ASN Processing: ${body.documentId}")
					.convertBodyTo(DocumentContext.class)
					.process(i -> i.getIn().getBody(DocumentContext.class).setAction("Process EDI ASN"))
					.process("lookupSupplyChainPurchaseOrder")
					.filter(i -> i.getIn().getBody(DocumentContext.class).getContext() == null)
						.log("Unable to resolve original document with ID: ${body.documentId}, no more to do")
						.stop()
					.end()
					.process("matchEdiDocument")
					.process("invoiceRequestProcessor")
					.log("Done ASN Processing.")
					.stop()
				.end()	
				.filter(i -> SourceDocumentType.Invoice.equals(i.getIn().getBody(EdiSourceDocument.class).getSourceDocumentType()))
					.log("Invoice Processing: ${body.documentId}")
					.process("invoiceDocumentPreProcessor")
					.convertBodyTo(DocumentContext.class)
					.process("lookupSupplyChainPurchaseOrder")
					.process(i -> i.getIn().getBody(DocumentContext.class).setAction("Process EDI Invoice"))
					.filter(i -> i.getIn().getBody(DocumentContext.class).getContext() == null)
						.log("Unable to resolve original document with ID: ${body.documentId}, no more to do")
						.stop()
					.end()
					.process("resolvePackslip")
					.process("orderPartLookup")
					.process("matchEdiDocument")
					.process("invoiceRequestProcessor")
//					.to("direct:lookup")
					.process("dropShipSupplierDetectionProcessor")
		        	.filter(i -> i.getProperty("isDropShipSupplier", false, Boolean.class))
		        		.log("Processing DropShip invoice.")
						.process("moaOrderToInvoiceProcessor")
						.process("dropshipSalesUpdate")
		        	.end()
					.to("direct:yoozFtp")
					.log("Done Invoice Processing.")
					.stop()
				.end()
			.end()
        .end();
        
        from("direct:ediFtp")
			.routeId("ediFtp")
        	.setProperty("context", body())
        	.setHeader("CamelFileName",simple("${body.documentId}.dat"))
            .convertBodyTo(EdiDocument.class)
		    .setBody(i -> i.getIn().getBody(EdiDocument.class).toString())
        	.to("{{ediDocumentArchive}}")
        	.log("Converting back to the context")
        	.setBody(i -> i.getProperty("context"))
    	.end();
        
        from("direct:ediSPSCommerce")
			.routeId("ediSPSCommerce")
    	    .setProperty("context", body())
    	    .setHeader("CamelFileName",simple("${body.documentId}.dat"))
    	    .convertBodyTo(EdiDocument.class)
    	    .setBody(i -> i.getIn().getBody(EdiDocument.class).toString())
    	    .to("{{SPSCommerceFtp}}")
    	    .log("Edi document successfully ftp`d to SPSCommerce")
    	    .setBody(i -> i.getProperty("context"))
    	.end();
        
        from("direct:yoozFtp")
        	.routeId("yoozFtp")
    		.setProperty("context", body())
    		.setHeader("CamelFileName",simple("${body.documentId}.csv"))
    		.process("lookupSupplyChainPurchaseOrder")
//             	.process("detectProcessedState")
    		.process("orderCustomerLookup")
    		.process("orderPartLookup")
//        		.convertBodyTo(DocumentContext.class)
    		.filter(i -> i.getIn().getBody(DocumentContext.class).getSourceDocument().getSourceDocumentType() == SourceDocumentType.PurchaseOrder)
    			.doTry()
	    			.process("yoozDocumentToCsv")
	    			.to("{{YoozFtp}}")
	    			.log("Document successfully ftp`d to Yooz")
	    			.doCatch(Exception.class)
	        		.log("Exception occurred : ${exception.message}")
	        	.doFinally()
	        		.setBody(i -> i.getProperty("context"))
	        	.end()	
    		.end()
    		.filter(i -> i.getIn().getBody(DocumentContext.class).getSourceDocument().getSourceDocumentType() == SourceDocumentType.Invoice)
    			.doTry()
	    			.process("yoozDocumentToDataContainer")
	    			.process("yoozSendInvoice")
	    			.log("Invoice document successfully sent to Yooz")
	    			.doCatch(Exception.class)
	        		.log("Exception occurred : ${exception.message}")
	        	.doFinally()
	        		.setBody(i -> i.getProperty("context"))
	        	.end()	
    		.end()
        		
        	
        .end();
        
        from("direct:yoozApi")
        	.routeId("yoozApi")
        	.process("yoozSendFiles")
        .end();
        
    }

}
