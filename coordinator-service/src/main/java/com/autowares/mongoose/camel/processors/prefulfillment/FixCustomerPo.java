package com.autowares.mongoose.camel.processors.prefulfillment;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorContext;

@Component
public class FixCustomerPo implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);

		if (context.getTransactionContext().getRequest() != null
				&& context.getTransactionContext().getRequest().getCustomerDocumentId() == null) {
			context.getTransactionContext().getRequest().setCustomerDocumentId(context.getCustomerDocumentId());
		}

	}

}
