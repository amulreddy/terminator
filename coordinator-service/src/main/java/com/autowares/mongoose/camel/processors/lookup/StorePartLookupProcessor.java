package com.autowares.mongoose.camel.processors.lookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.geo.model.GeoLocationResult;
import com.autowares.mongoose.command.StoreInventoryCommand;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.stockcheck.InventoryResponseMulti;
import com.mapbox.geojson.Feature;

@Component
public class StorePartLookupProcessor implements Processor {

	private static Logger log = LoggerFactory.getLogger(StorePartLookupProcessor.class);
	private static int maxNumberOfStores = 11;

	private StoreInventoryCommand storeInventoryCommand = new StoreInventoryCommand();

	@Override
	public void process(Exchange exchange) throws Exception {
		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);
		List<String> storesToInquire = new ArrayList<>();

		storesToInquire.addAll(context.getFulfillmentOptions().getPreferredLocations().stream()
				.collect(Collectors.toList()));

		if (storesToInquire.isEmpty() && context.getBusinessContext().getPhysicalContext() != null) {
			GeoLocationResult spatialData = context.getBusinessContext().getPhysicalContext().getSpatialData();
			if (spatialData != null) {
				for (Feature f : spatialData.getNearFeature().get("Parts Store").features()) {
					if (storesToInquire.size() < maxNumberOfStores) {
						if (!f.getStringProperty("name").equals(String.valueOf(context.getCustomerNumber()))) {
							storesToInquire.add(f.getStringProperty("name"));
						}
					}
				}
			}
		}

		Map<String, CompletableFuture<InventoryResponseMulti>> storeResultMap = new HashMap<String, CompletableFuture<InventoryResponseMulti>>();
		for (String storeName : storesToInquire) {
			log.info("Getting " + storeName + " inventory for customer: " + context.getCustomerNumber());
			// TODO I want to lookup all details at all stores in one call...
			CompletableFuture<InventoryResponseMulti> futureDetails = storeInventoryCommand
					.lookupStoreAvailability(context.getLineItems(), storeName);
			storeResultMap.put(storeName, futureDetails);
		}

		for (Entry<String, CompletableFuture<InventoryResponseMulti>> e : storeResultMap.entrySet()) {
			storeInventoryCommand.getStoreAvailability(context.getLineItems(), e.getValue(), e.getKey());
		}

		exchange.getIn().setBody(context);

	}

}
