package com.autowares.mongoose.camel.processors.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.servicescommon.model.FulfillmentOptions;

@Component
public class WarehouseExclusions implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {

		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);
		Object o = exchange.getIn().getBody(Object.class);
//		FulfillmentLocationContext orderFillContext = null;
		if (o instanceof DocumentContext) {
//			context = exchange.getIn().getBody(DocumentContext.class).getContext();
//			orderFillContext = exchange.getIn().getBody(DocumentContext.class).getFulfillmentLocationContext();
		}
		
		FulfillmentOptions fulfillmentOptions = context.getFulfillmentOptions();
		List<String> excludedLocations = fulfillmentOptions.getExcludedLocations();
		if (excludedLocations == null) {
			excludedLocations = new ArrayList<>();
		}
		
		String shipTo = context.getBusinessContext().getAccountNumber();
		String destinationLocation = context.getBusinessContext().getBusinessDetail().getServicingWarehouse().getBuildingMnemonic();
//		String sourceLocation = orderFillContext.getLocation();

		if (!destinationLocation.equals("GAY")) {
			addExcludedLocation(excludedLocations, "GAY");
			// Add restrictions - remove Gaylord fulfillmentLocations
		}
		
		if (!destinationLocation.equals("MAD")) {
			addExcludedLocation(excludedLocations, "MAD");
			// Add restrictions - remove Madison fulfillmentLocations
		}
		
		if (destinationLocation.equals("GAY") || destinationLocation.equals("MAD")) {
				addExcludedLocation(excludedLocations, "LAY");
				addExcludedLocation(excludedLocations, "LOU");
		}

		if (fulfillmentOptions != null) {
			addExcludedLocation(excludedLocations, "A1T");
			addExcludedLocation(excludedLocations, "XXX");
			fulfillmentOptions.setExcludedLocations(excludedLocations);
		}

	}

	private void addExcludedLocation(List<String> excludedLocations, String location) {
		if (!excludedLocations.contains(location)) {
			excludedLocations.add(location);
		}
	}
}
