package com.autowares.mongoose.camel.processors.prefulfillment;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.LineItemContext;

@Component
public class OrderQuantityAdjuster implements Processor {

	private static Logger log = LoggerFactory.getLogger(OrderQuantityAdjuster.class);
	
	@Override
	public void process(Exchange exchange) throws Exception {
		CoordinatorContext orderContext = exchange.getIn().getBody(CoordinatorContext.class);
		
		for (LineItemContext orderDetail : orderContext.getLineItems()) {
			
			if (orderDetail.getQuantity() == null) {
				orderDetail.setQuantity(1);
			}
			
			if (orderDetail.getQuantity() > 9999) {
				orderDetail.setQuantity(1);
			}
			
			if (orderDetail.getPart() != null) {
				Double multiplier = null;
				try {
					multiplier = orderDetail.getPart().getCwMultiplier().doubleValue();
				} catch (Exception e) {
				}
				
				try {
					Integer orderQuantity = orderDetail.getQuantity();
					Integer packageQuantity = orderDetail.getPart().getPackageQuantity();
					Integer newQuantity = orderQuantity;
					Integer minSellQty = Math.max(orderDetail.getPart().getMinWarehouseSellQty(), 1);
					String message = null;
					// Adjusting the order quantity based on the counter works multiplier and
					// package quantity.
					if (multiplier != null && multiplier != 1 && multiplier != 0 && packageQuantity != null) {
						if (orderQuantity > 1 && packageQuantity > 1 && orderQuantity % packageQuantity == 0) {
							if (orderQuantity % multiplier == 0) {
								if (multiplier < 1) {
									newQuantity = (int) Math.max((orderQuantity * multiplier), 1);
								} else {
									newQuantity = (int) Math.max((orderQuantity / multiplier), 1);
								}
							} else {
								if (multiplier < 1) {
									newQuantity = (int) Math.max(Math.ceil(orderQuantity * multiplier), 1);
								} else {
									newQuantity = (int) Math.max((orderQuantity / multiplier), 1);
								}
							}
							message = "Changing order quantity from " + orderQuantity + " to " + newQuantity + " for "
									+ orderDetail.getVendorCode() + " " + orderDetail.getPartNumber()
									+ " with package qty of " + packageQuantity + " and multiplier of " + multiplier;
						}
					}
					// Adjusting the order quantity based on the warehouse minimum selling quantity.
					if (newQuantity % minSellQty != 0) {
						newQuantity = ((int) (newQuantity / minSellQty) + 1) * minSellQty;
						message = "Changing order quantity from " + orderQuantity + " to " + newQuantity + " for "
								+ orderDetail.getVendorCode() + " " + orderDetail.getPartNumber()
								+ " with Minimum Sell Quantity of " + minSellQty;
					}
					if (newQuantity != orderQuantity) {
						orderDetail.setOriginalQuantity(orderQuantity);
						orderDetail.setQuantity(newQuantity);
						log.info(message);
						orderDetail.updateOrderLog(message);
					}
				} catch (Exception e) {
					log.error("Failed to adjust quantity", e);
				}
			}

		}

	}

}
