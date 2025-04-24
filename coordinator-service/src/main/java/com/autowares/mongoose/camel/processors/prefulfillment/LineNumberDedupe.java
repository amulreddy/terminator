package com.autowares.mongoose.camel.processors.prefulfillment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.LineItemContextImpl;

@Component
public class LineNumberDedupe implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);

		List<LineItemContextImpl> lineItems = new ArrayList<>();

		for (LineItemContextImpl lineItem : context.getLineItems()) {
			Optional<LineItemContextImpl> item = lineItems.stream().filter(i -> i.getProductId() != null)
					.filter(i -> i.getProductId().equals(lineItem.getProductId())).findAny();
			if (item.isPresent()) {
				item.get().setQuantity(lineItem.getQuantity() + item.get().getQuantity());
				item.get().updateOrderLog("Merged duplicate order items -> combined order quantities");
				context.updateProcessingLog("line= " + lineItem.getLineCode() + " part= " + lineItem.getPartNumber()
						+ "  Merged duplicate order items -> combined order quantities, new quantity = "
						+ item.get().getQuantity());
			} else {
				lineItems.add(lineItem);
			}
		}

		context.setLineItems(lineItems);

	}

}
