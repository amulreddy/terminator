package com.autowares.mongoose.camel.processors.errorhandling;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.FulfillmentContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.servicescommon.model.WorkingDocument;

@Component
public class MarkContextAsError implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {

	    WorkingDocument context = exchange.getIn().getBody(WorkingDocument.class);

		if (context == null) {
			LineItemContext lineItemContext = exchange.getIn().getBody(LineItemContext.class);
			if (lineItemContext != null && lineItemContext.getContext() instanceof CoordinatorContext) {
				context = (CoordinatorContext) lineItemContext.getContext();
			}
		}
		
		if (context == null) {
			FulfillmentContext fillDetail = exchange.getIn().getBody(FulfillmentContext.class);
			if (fillDetail != null && fillDetail.getLineItem().getContext() instanceof CoordinatorContext) {
				context = (CoordinatorContext) fillDetail.getLineItem().getContext();
			}
		}

		if (context != null) {
			context.setTransactionStage(TransactionStage.Done);
			context.setTransactionStatus(TransactionStatus.Error);
		}
	}

}
