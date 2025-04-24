package com.autowares.mongoose.camel.processors.integration;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.model.MoaOrderLineItemContext;
import com.autowares.mongoose.service.ViperToWmsCoreUtils;
import com.autowares.orders.model.Callback;
import com.autowares.orders.model.Item;

@Component
public class ManagerHandledShortFulfillmentCallback implements Processor {

	@Autowired
	ViperToWmsCoreUtils coreUtils;

	@Override
	public void process(Exchange exchange) throws Exception {
		CoordinatorContext subContext = exchange.getIn().getBody(CoordinatorContext.class);
		CoordinatorContext source2Context = subContext.getSourceContext().getSourceContext();

		for (LineItemContext lineItemContext : subContext.getLineItems()) {

			Optional<? extends LineItemContext> originalOptionalLineItemContext = source2Context.getLineItems().stream()
					.filter(i -> i.getLineNumber().equals(lineItemContext.getLineNumber())).findAny();
			if (originalOptionalLineItemContext.isPresent()) {
				MoaOrderLineItemContext originalLineItemContext = (MoaOrderLineItemContext) originalOptionalLineItemContext
						.get();
				Item orderItem = originalLineItemContext.getInvoiceItem();
				customerCallback(orderItem);
			}
		}

	}

	/**
	 * Create new customer callback record.
	 */
	private void customerCallback(Item orderItem) {
		Callback callbackRequest = new Callback();
		callbackRequest.setOrderId(orderItem.getOrderItemId().longValue());
		callbackRequest.setZeroTime(ZonedDateTime.now());
		coreUtils.getClient().customerCallback(callbackRequest);

	}
}
