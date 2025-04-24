package com.autowares.mongoose.camel.processors.prefulfillment;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.FulfillmentContext;
import com.autowares.mongoose.model.LineItemContext;

@Component
public class FulfillmentItemPopulator implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		CoordinatorContext orderContext = exchange.getIn().getBody(CoordinatorContext.class);

		for (LineItemContext lineItem : orderContext.getLineItems()) {
			if (lineItem.getFulfillmentDetails().isEmpty() && lineItem.getQuantity() !=null) {
				for (int i = 0; i < lineItem.getQuantity(); i++) {
					new FulfillmentContext(lineItem);
				}
			}
		}
	}

}
