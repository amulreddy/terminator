package com.autowares.mongoose.camel.processors.postfulfillment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.apis.partservice.PartBase;
import com.autowares.mongoose.camel.components.OrderFillContextTypeConverter;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.model.TransactionalContext;
import com.autowares.partpricing.command.PartPricing;
import com.autowares.partpricing.model.OrderPriceLineItem;
import com.autowares.partpricing.model.OrderPriceRequest;
import com.autowares.partpricing.model.PartPrice;
import com.autowares.servicescommon.model.FulfillmentLocation;
import com.autowares.servicescommon.model.LineItem;
import com.autowares.servicescommon.model.Order;
import com.autowares.servicescommon.model.PartLineItemImpl;
import com.autowares.servicescommon.model.PriceLevel;

@Component
public class WarehouseOrderPricing implements Processor {

	private static Logger log = LoggerFactory.getLogger(WarehouseOrderPricing.class);

	PartPricing partPricingClient = new PartPricing();

	@Override
	public void process(Exchange exchange) throws Exception {
		Object object = exchange.getIn().getBody();
		CoordinatorContext context = null;
		if (object instanceof FulfillmentLocationContext) {
			FulfillmentLocationContext fulfillmentContext = (FulfillmentLocationContext) object;
			context = fulfillmentContext.getOrder();
		}
		if (object instanceof CoordinatorContext) {
			context = (CoordinatorContext) object;
		}
		if(object instanceof DocumentContext) {
			context = ((DocumentContext)object).getContext();
		}

		Boolean dealWithInterchanges = false;

		if (context instanceof CoordinatorOrderContext) {
			CoordinatorOrderContext orderContext = (CoordinatorOrderContext) context;
			log.info("Pricing order: " + orderContext.getXmlOrderId() + " " + " source: "
					+ orderContext.getOrderSource());
		}

		if (context.getInquiryOptions() != null && context.getInquiryOptions().getIncludeInterchanges()) {
			dealWithInterchanges = true;

		}

		OrderPriceRequest orderPriceRequest = new OrderPriceRequest(context.getBuyingAccount(), new ArrayList<>());
		for (LineItemContext lineItem : context.getLineItems()) {
//		    TODO - why are there multiple fulfillment locations?
			List<Availability> locations = lineItem.getAvailability().stream().filter(i -> i.getFillQuantity() > 0)
					.collect(Collectors.toList());
			OrderPriceLineItem orderPriceLineItem = new OrderPriceLineItem(lineItem);
			orderPriceRequest.getLineItems().add(orderPriceLineItem);
			for (Availability location : locations) {
				FulfillmentLocation fulfillmentLocation = OrderFillContextTypeConverter
						.orderFillContextToFreight(location.getFulfillmentLocation());
				fulfillmentLocation.setHandlingPriorityType(location.getHandlingPriorityType());
				orderPriceLineItem.getFulfillmentLocations().add(fulfillmentLocation);
			}
		}

		if (dealWithInterchanges) {
			List<OrderPriceLineItem> interchanges = context.getLineItems().stream()
					.filter(i -> i.getPart() != null && i.getPart().getInterchanges() != null
							&& !i.getPart().getInterchanges().isEmpty())
					.map(i -> i.getPart().getInterchanges()).flatMap(List::stream)
					.map(i -> new OrderPriceLineItem(new PartLineItemImpl(i.getProductId(), 1)))
					.collect(Collectors.toList());
			orderPriceRequest.getLineItems().addAll(interchanges);
			log.info("Pricing all interchanges: " + interchanges.size());
		}

		orderPriceRequest.setIncludePriceLevels(true);
		Order pricedOrder = partPricingClient.priceOrder(orderPriceRequest);

		if (pricedOrder != null) {

			for (LineItemContext detail : context.getLineItems()) {
				for (LineItem item : pricedOrder.getLineItems()) {
					if (detail.getProductId() != null && item.getProductId() != null
							&& detail.getProductId().equals(item.getProductId())) {

						BigDecimal price = detail.getPrice();
						if (price == null) {
							price = item.getPrice();
						} else if (price.compareTo(item.getPrice()) != 0) {
							context.updateProcessingLog("Using overridden price for line number: "
									+ detail.getLineNumber() + "  price = " + price);
						}
						detail.setPrice(price);
						if (detail.getPart() != null) {
							detail.getPart().setPrice(price);
						}

					}
				}

				if (dealWithInterchanges) {
					if (detail.getPart() != null && detail.getPart().getInterchanges() != null) {
						for (PartBase interchange : detail.getPart().getInterchanges()) {
							for (LineItem item : pricedOrder.getLineItems()) {
								if (interchange.getProductId() != null && item.getProductId() != null
										&& interchange.getProductId().equals(item.getProductId())) {
									interchange.setPrice(item.getPrice());
								}
							}
						}
					}
				}
			}

			if (context.getProcurementGroupContext() != null
					&& context.getProcurementGroupContext().getSupplierContext() != null) {
				TransactionalContext supplierContext = context.getProcurementGroupContext().getSupplierContext();
				if (supplierContext.getOrderContext() != null) {
					CoordinatorContext supplierOrderContext = supplierContext.getOrderContext();
					for (LineItemContext lineItem : supplierOrderContext.getLineItems()) {
						Optional<? extends LineItem> optionalPricedLine = pricedOrder.getLineItems().stream().filter(
								i -> i.getProductId() != null && i.getProductId().equals(lineItem.getProductId()))
								.findAny();
						if (optionalPricedLine.isPresent()) {
							LineItem priceLine = optionalPricedLine.get();
							if (priceLine instanceof PartPrice) {
								PartPrice partPrice = (PartPrice) priceLine;
								Optional<PriceLevel> wdInvoice = partPrice.getPriceLevels().stream()
										.filter(i -> "WD_INV_COST".equals(i.getPriceFieldCode())).findAny();
								if (wdInvoice.isPresent()) {
									lineItem.setPrice(wdInvoice.get().getCurrentPrice());
								}
							}
						}
					}
				}

			}

		}
	}
}
