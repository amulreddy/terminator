package com.autowares.mongoose.camel.processors.nonstock;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;

@Component
public class ManualQuoteProcessor implements Processor {

	private static Logger log = LoggerFactory.getLogger(ManualQuoteProcessor.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		FulfillmentLocationContext fulfillmentLocationContext = exchange.getIn()
				.getBody(FulfillmentLocationContext.class);

		if (fulfillmentLocationContext.getNonStockContext() == null) {
			throw new AbortProcessingException("No non stock context found.");
		}

		CoordinatorContext nonStockContext = fulfillmentLocationContext.getNonStockContext();

		CoordinatorContext coordinatorContext = nonStockContext.getProcurementGroupContext().getCustomerContext()
				.getQuoteContext();

		CoordinatorContext supplierContext = nonStockContext.getProcurementGroupContext().getSupplierContext()
				.getQuoteContext();

		coordinatorContext.setTransactionStage(TransactionStage.Open);
		coordinatorContext.setTransactionStatus(TransactionStatus.Processing);
		nonStockContext.setTransactionStatus(TransactionStatus.Processing);
		supplierContext.setTransactionStatus(TransactionStatus.Accepted);
		
		
		log.info("Manual quote processor.");
		log.info("Set the status to processing for manual.");


	}

}