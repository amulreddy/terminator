package com.autowares.mongoose.camel.processors.util;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.servicescommon.model.FulfillmentOptions;
import com.autowares.servicescommon.model.InquiryOptions;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.servicescommon.model.WorkingDocument;

@Component
public class UpdateWorkingState implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		Object object = exchange.getIn().getBody();
		WorkingDocument workingDocument = null;
		if (object instanceof WorkingDocument) {
			workingDocument = (WorkingDocument) object;
		}
		if (object instanceof DocumentContext) {
			DocumentContext documentContext = (DocumentContext) object;
			workingDocument = (WorkingDocument) documentContext.getFulfillmentLocationContext();
		}
		TransactionStatus currentStatus = workingDocument.getTransactionStatus();
		TransactionStage currentStage = workingDocument.getTransactionStage();
		if (TransactionStage.Ready.equals(currentStage)) {
			workingDocument.setTransactionStage(TransactionStage.Open);
		}

		if (currentStatus == null || TransactionStatus.Processing.equals(currentStatus)) {
			workingDocument.setTransactionStatus(TransactionStatus.Accepted);
		}
		
		if (workingDocument instanceof CoordinatorContext) {
			CoordinatorContext coordinatorContext = (CoordinatorContext) workingDocument;
			FulfillmentOptions fulfillmentOptions = coordinatorContext.getFulfillmentOptions();
			InquiryOptions inquiryOptions = coordinatorContext.getInquiryOptions();
			if (fulfillmentOptions.getQuoteNonStockItems()) {
				Boolean hasManualFulfillment = coordinatorContext.getFulfillmentSequence().stream().filter(i -> SystemType.Manual.equals(i.getSystemType())).findAny().isPresent();
				if (hasManualFulfillment || inquiryOptions.getInternalOnly()) {
					workingDocument.setTransactionStatus(TransactionStatus.Processing);
				}
			}
		}
//		for (LineItemContext lineItemContext : coordinatorContext.getLineItems()) {
//			Set<FulfillmentLocationContext> fulfillmentLocations = new HashSet<>();
//			for (FulfillmentContext orderFillDetail : lineItemContext.getFulfillmentDetails()) {
//				fulfillmentLocations.add(orderFillDetail.getFulfillmentLocation());
//			}
//			if (fulfillmentLocations.size() == 1
//					&& SystemType.Manual.equals(fulfillmentLocations.iterator().next().getSystemType())) {
//				coordinatorContext.setTransactionStage(TransactionStage.New);
//			}
//			//TODO Handle error state.
//		}
	}

}
