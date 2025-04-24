package com.autowares.mongoose.coordinator;

import org.junit.jupiter.api.Test;

import com.autowares.mongoose.model.MoaOrderContext;
import com.autowares.mongoose.model.MoaOrderLineItemContext;
import com.autowares.orders.model.Item;
import com.autowares.orders.model.Order;
import com.autowares.orders.model.ProcessStage;
import com.autowares.servicescommon.util.PrettyPrint;
import com.autowares.xmlgateway.model.OrderRequest;

public class BuildOrderRequestTest {

	@Test
	public void buildOrderRequest() {
		OrderRequest orderRequest = new OrderRequest();
		PrettyPrint.print(orderRequest);
	}

	@Test
	public void testEquality() {

		Order order = new Order();
		Item item = new Item();

		MoaOrderContext moaOrderContext = new MoaOrderContext(order);

		MoaOrderLineItemContext moaOrderLineItemContext = new MoaOrderLineItemContext(moaOrderContext, item);
		item.setWarehouse(6);
		if (item.getWarehouse() == 6) {
			PrettyPrint.print("warehouse=6");
		} else {
			PrettyPrint.print("no match.");
		}
		item.setMsErrorCode("4");
		item.setMsErrorCodeDescription("The ship quantity requested for the order is not available (via coordinator).");
		item.setProcessStage(ProcessStage.Canceled);
		System.out.println(item.getMsErrorCodeDescription());
	}

}
