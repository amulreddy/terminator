package com.autowares.mongoose.camel.processors.fulfillmentplanning;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.model.FulfillmentContext;
import com.autowares.servicescommon.util.PrettyPrint;

@Component
public class FulfillmentPlanComparor implements Processor {

	private static Logger log = LoggerFactory.getLogger(FulfillmentPlanComparor.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		CoordinatorOrderContext context = exchange.getIn().getBody(CoordinatorOrderContext.class);

		log.warn("Comparing order: " + context.getXmlOrderId() + " " + " source: " + context.getOrderSource());

		for (LineItemContext detail : context.getLineItems()) {
			if (detail.getPlannedFulfillment() != null) {
				Map<String, Long> currentPlan = detail.getFulfillmentDetails().stream()
						.filter(i -> i.getFillQuantity() > 0)
						.collect(Collectors.groupingBy(FulfillmentContext::getLocation,
								Collectors.summingLong(FulfillmentContext::getFillQuantity)));
				Map<String, Long> optaplannerPlan = detail.getPlannedFulfillment().getFulfillmentMap();

				if (currentPlan.equals(optaplannerPlan)) {
					log.warn("No difference in lineItem: " + detail.getLineNumber());
				} else {
					log.warn("Line difference: " + detail.getLineNumber());
					log.warn("Current: " + PrettyPrint.toString(currentPlan));
					log.warn("Optaplanner: " + PrettyPrint.toString(optaplannerPlan));
					log.warn(PrettyPrint.toString(detail.getPlannedFulfillment()));
				}
			}
		}

		exchange.getIn().setBody(exchange.getIn().getBody());
	}

}
