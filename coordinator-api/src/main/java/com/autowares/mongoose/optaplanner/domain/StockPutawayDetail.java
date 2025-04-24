package com.autowares.mongoose.optaplanner.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import com.fasterxml.jackson.annotation.JsonIgnore;


@PlanningEntity
public class StockPutawayDetail {
	
	private StockingLocation location;
	private StockDetail detail;

	@PlanningVariable(valueRangeProviderRefs = "locationRange", nullable = true)
	public StockingLocation getLocation() {
		return location;
	}

	@JsonIgnore
	public Boolean hasLocation() {
		return location != null;
	}

	public void setLocation(StockingLocation location) {
		if (location != null) {
			if (this.location != null) {
				this.location.getPutawayDetails().remove(this);
			}
			location.getPutawayDetails().add(this);
		}
		this.location = location;
	}

	public StockDetail getDetail() {
		return detail;
	}

	public void setDetail(StockDetail detail) {
		this.detail = detail;
		detail.getPutawayDetails().add(this);
	}

	@JsonIgnore
	public Integer getArrivalDate() {
		if (this.location != null) {
			return this.location.getArrivalDateAsInt();
		}
		return Integer.MAX_VALUE;
	}

	@Override
	public String toString() {
		if (detail != null) {
			return String.valueOf(detail.getDetailId());
		}
		return null;
	}

}
