package com.autowares.mongoose.camel.processors.lookup;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.mongoose.model.MoaOrderContext;
import com.autowares.mongoose.model.MoaOrderLineItemContext;
import com.autowares.orders.clients.OrderClient;
import com.autowares.orders.model.Item;
import com.autowares.orders.model.ProcessStage;
import com.autowares.orders.model.RestResponsePage;
import com.google.common.collect.Lists;

@Component
public class FindZeroShipOrders implements Processor {

	private OrderClient orderClient = new OrderClient();

	@Override
	public void process(Exchange exchange) throws Exception {

		List<MoaOrderContext> coordinatorContextList = new ArrayList<>();
		// Retrieve a list of moa_orders where the part is allocated and the shipqty =
		// 0.
		RestResponsePage<Item> zeroedItems = orderClient.findItems(null, null, null, null, null,
				Lists.newArrayList(ProcessStage.Allocated), null, null, 0, null, 500);
		for (Item item : zeroedItems.getContent()) {
			MoaOrderContext coordinatorContext = null;
			for (MoaOrderContext cc : coordinatorContextList) {
				if (cc.getOrder().getOrderId().equals(item.getOrder().getOrderId())) {
					Optional<LineItemContextImpl> containsLineNumber = cc.getLineItems().stream()
							.filter(i -> i.getLineNumber().equals(item.getCustomerLineNumber())).findAny();
					if (!containsLineNumber.isPresent()) {
						// If the line number isn't already contained within this context we can use it,
						// otherwise we need to create a new context to avoid conflicts in part lookup.
						coordinatorContext = cc;
						break;
					}
				}
			}
			
			if (coordinatorContext == null) {
				coordinatorContext = new MoaOrderContext(item.getOrder());
				coordinatorContextList.add(coordinatorContext);
				MoaOrderLineItemContext tmpLineItem = new MoaOrderLineItemContext(coordinatorContext, item);
				LineItemContextImpl lineItem = new LineItemContextImpl(tmpLineItem);
				FulfillmentLocationContext fulfillmentLocation = new FulfillmentLocationContext(coordinatorContext,
						"Not Available");
				Availability availablity = new Availability(lineItem, fulfillmentLocation);
				coordinatorContext.getLineItems().add(lineItem);
				availablity.setMoaOrderDetail(item);
			} else {
				MoaOrderLineItemContext tmpLineItem = new MoaOrderLineItemContext(coordinatorContext, item);
				LineItemContextImpl lineItem = new LineItemContextImpl(tmpLineItem);
				FulfillmentLocationContext fulfillmentLocation = coordinatorContext.getFulfillmentSequence().stream()
						.filter(i -> "Not Available".equals(i.getLocation())).findAny().get();
				Availability availablity = new Availability(lineItem, fulfillmentLocation);
				coordinatorContext.getLineItems().add(lineItem);
				availablity.setMoaOrderDetail(item);
			}
		}

		exchange.getIn().setBody(coordinatorContextList);

	}

}
