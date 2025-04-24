package com.autowares.mongoose.optaplanner.domain;


import java.util.ArrayList;
import java.util.List;

import org.optaplanner.core.api.domain.lookup.PlanningId;

import com.autowares.supplychain.model.optaplanner.OptaplannerFulfillmentLocation;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class FulfillmentLocation extends OptaplannerFulfillmentLocation {

	private List<OrderFillDetail> fulfillments = new ArrayList<>();

	@PlanningId
	public String getLocationName() {
		return super.getLocationName();
	}

	@JsonIgnore
	public List<OrderFillDetail> getFulfillments() {
		return fulfillments;
	}

	public void setFulfillments(List<OrderFillDetail> fulfillments) {
		this.fulfillments = fulfillments;
	}

	

}
