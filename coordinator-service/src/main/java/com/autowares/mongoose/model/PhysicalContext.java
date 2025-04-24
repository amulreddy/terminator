package com.autowares.mongoose.model;

import java.util.HashMap;
import java.util.Map;

import com.autowares.geo.model.GeoLocationResult;
import com.autowares.logistix.model.ShipmentPlanning;
import com.mapbox.geojson.Feature;

public class PhysicalContext {

	private Feature feature;
	private GeoLocationResult spatialData;
	private Map<String, ShipmentPlanning> fulfillmentDistanceMatrix = new HashMap<>();

	public Feature getFeature() {
		return feature;
	}

	public void setFeature(Feature feature) {
		this.feature = feature;
	}

	public GeoLocationResult getSpatialData() {
		return spatialData;
	}

	public void setSpatialData(GeoLocationResult spatialData) {
		this.spatialData = spatialData;
	}

	public Map<String, ShipmentPlanning> getFulfillmentDistanceMatrix() {
		return fulfillmentDistanceMatrix;
	}

	public void setFulfillmentDistanceMatrix(Map<String, ShipmentPlanning> fulfillmentDistanceMatrix) {
		this.fulfillmentDistanceMatrix = fulfillmentDistanceMatrix;
	}

}
