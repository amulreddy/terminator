package com.autowares.mongoose.camel.processors.lookup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import com.autowares.geo.commands.FeatureClient;
import com.autowares.geo.model.Distance;
import com.autowares.geo.model.GeoLocationResult;
import com.autowares.logistix.model.ShipmentPlanning;
import com.autowares.mongoose.model.BusinessContext;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.PhysicalContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;

@Component
public class OrderPhysicalLocationLookup implements Processor {

	private FeatureClient featureClient = new FeatureClient();
	private static Logger log = LoggerFactory.getLogger(OrderPhysicalLocationLookup.class);
	private Map<String, Feature> warehouseFeatures = new HashMap<>();
	
	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public void process(Exchange exchange) throws Exception {
		log.info("Looking up physical location data");
		CoordinatorContext request = exchange.getIn().getBody(CoordinatorContext.class);
		Map<String, ShipmentPlanning> fulfillmentDistanceMatrix = new HashMap<>();

		// TODO - Dynamically build campus locations list
		List<String> warehouseBuildings = Lists.newArrayList("GRR", "GAY", "MAD", "CWT", "WRN", "FLT", "A1T", "CHI");

		for (String building : warehouseBuildings) {
			fulfillmentDistanceMatrix.put(building, null);
			Optional<Feature> optionalFeature = featureClient.getFeatureByName(building);
			if (optionalFeature.isPresent()) {
				if (!warehouseFeatures.containsKey(building)) {
					warehouseFeatures.put(building, optionalFeature.get());
				}
			}
		}

		if (request.getBusinessContext() != null) {
			StopWatch sw = new StopWatch();
			sw.start("Looking up geoResult");

			BusinessContext businessContext = request.getBusinessContext();
			GeoLocationResult geoResult = null;
			try {
				geoResult = featureClient.getFeaturesSpatialByName(String.valueOf(request.getCustomerNumber()), Lists.newArrayList(2963l, 2976l), Optional.of(500000l));
			} catch (RuntimeException e) {
				
			}
					
			if (geoResult != null) {
				sw.stop();
				log.info(sw.getLastTaskName() + " took: " + sw.getLastTaskTimeMillis() + "ms");
				sw.start("Looking up Distances");

				Feature feature = geoResult.getFeature();
				FeatureCollection fc = null;
				if (feature != null && feature.id() != null) {
					fc = featureClient.getDistance(Long.valueOf(feature.id()), warehouseFeatures.values().stream()
							.map(i -> Long.valueOf(i.id())).collect(Collectors.toList()), false);
				}
				if (fc != null) {
					for (Feature f : fc.features()) {
						try {
							String distance = f.getProperty("distance").toString();
							Distance d = objectMapper.readValue(distance,Distance.class);
							ShipmentPlanning p = new ShipmentPlanning();
							p.setTravelTime(d.getDuration());
							fulfillmentDistanceMatrix.put(f.getStringProperty("name"), p);
						} catch (Exception e) {
							log.error("Unable to calculate distance");
						}						
					}
				} else {
					log.error("No Distances found");
				}
				sw.stop();
				log.info(sw.getLastTaskName() + " took: " + sw.getLastTaskTimeMillis() + "ms");
				PhysicalContext physicalContext = new PhysicalContext();
				physicalContext.setFeature(feature);
				physicalContext.setSpatialData(geoResult);
				physicalContext.setFulfillmentDistanceMatrix(fulfillmentDistanceMatrix);
				businessContext.setPhysicalContext(physicalContext);
			}

		}
		exchange.getIn().setBody(request);
	}

}
