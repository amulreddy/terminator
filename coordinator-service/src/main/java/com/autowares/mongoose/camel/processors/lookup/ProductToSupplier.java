package com.autowares.mongoose.camel.processors.lookup;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.apis.ids.command.LocationLookupClient;
import com.autowares.apis.ids.model.BusinessDetail;
import com.autowares.mongoose.model.BusinessContext;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorContextImpl;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.servicescommon.model.PartLineItem;

@Component
public class ProductToSupplier implements Processor {

	LocationLookupClient locationLookupClient = new LocationLookupClient();

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
		if (object instanceof DocumentContext) {
			DocumentContext documentContext = (DocumentContext)object;
			context = documentContext.getContext();
		}

		Map<String, List<LineItemContext>> vcscMap = context.getLineItems().stream().filter(i -> i instanceof PartLineItem)
				.collect(Collectors.groupingBy(PartLineItem::getVendorCodeSubCode));
		
		for (Entry<String, List<LineItemContext>> entry : vcscMap.entrySet()) {
			BusinessDetail business = locationLookupClient.getBusinessByVendorCodeSubCode(entry.getKey());
			if (business.getVendorMaster() != null) {
				for (LineItemContext lineItemContext : entry.getValue()) {
					lineItemContext.setVendorMaster(business.getVendorMaster());
				}
			}
			if (context instanceof CoordinatorContextImpl) {
				BusinessContext businessContext = new BusinessContext();
				businessContext.setBusinessDetail(business);
				context.setSupplier(businessContext);
			}
		}
	}
}
