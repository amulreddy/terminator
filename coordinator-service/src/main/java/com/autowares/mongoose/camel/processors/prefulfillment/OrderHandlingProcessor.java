package com.autowares.mongoose.camel.processors.prefulfillment;

import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.apis.ids.model.WarehouseMaster;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.FulfillmentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.OrderHandling;
import com.autowares.servicescommon.model.RunType;

@Component
public class OrderHandlingProcessor implements Processor {


	@Override
	public void process(Exchange exchange) throws Exception {
		FulfillmentLocationContext fulfillmentContext = exchange.getIn().getBody(FulfillmentLocationContext.class);

		RunType deliveryRunType = fulfillmentContext.getDeliveryRunType();
		Optional<WarehouseMaster> warehouseMasterOptional = Optional.of(fulfillmentContext.getWarehouseMaster());

		for (FulfillmentContext fulfillment : fulfillmentContext.getFulfillmentDetails()) {

			if (warehouseMasterOptional.isPresent()) {
				WarehouseMaster warehouseMaster = warehouseMasterOptional.get();
				Boolean bulk = false;
				Availability availability = fulfillment.getAvailability();

				if (availability !=null
						&& availability.getPartAvailability().getBulkItem() != null) {
					bulk = availability.getPartAvailability().getBulkItem();
				}
				Boolean mustgo = false;
				if (fulfillment.getLineItem().getMustGo() != null) {
					mustgo = fulfillment.getLineItem().getMustGo();
				}

				if (RunType.expressDelivery.equals(deliveryRunType) || RunType.expressPickup.equals(deliveryRunType)) {
					if (!bulk && !warehouseMaster.getRfPullBinExpress()) {
						fulfillment.setOrderHandling(OrderHandling.Print);
					}
					if (bulk && !warehouseMaster.getRfPullBulkExpress()) {
						fulfillment.setOrderHandling(OrderHandling.Print);
					}
				}
				if (RunType.pickup.equals(deliveryRunType)) {
					if (!warehouseMaster.getRfPickupProcessingLocation()) {
						fulfillment.setOrderHandling(OrderHandling.Print);
					}
				}
				if (mustgo) {
					if (!bulk && !warehouseMaster.getRfPullBinNotifyIfIncomplete()) {
						fulfillment.setOrderHandling(OrderHandling.Print);
					}
					if (bulk && !warehouseMaster.getRfPullBulkNotifyIfIncomplete()) {
						fulfillment.setOrderHandling(OrderHandling.Print);
					}
				}
				if (warehouseMaster.getPrintNowOrdersOnly()) {
					fulfillment.setOrderHandling(OrderHandling.Print);
				}
			}
		}
	}
}
