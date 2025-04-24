package com.autowares.mongoose.camel.components;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;

public class SplitFulfillmentAggregationStrategy implements AggregationStrategy {

	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		// put order together in old exchange by adding the order from new exchange

		FulfillmentLocationContext orderFillContext = newExchange.getIn().getBody(FulfillmentLocationContext.class);
		CoordinatorContext order = orderFillContext.getOrder();

		if (oldExchange != null) {
			oldExchange.getIn().setBody(order);
			return oldExchange;
		} else {
			newExchange.getIn().setBody(order);
			return newExchange;
		}

	}
}