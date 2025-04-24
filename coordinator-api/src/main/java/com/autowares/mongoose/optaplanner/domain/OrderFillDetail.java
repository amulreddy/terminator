package com.autowares.mongoose.optaplanner.domain;

import java.util.Objects;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import com.fasterxml.jackson.annotation.JsonIgnore;


@PlanningEntity
public class OrderFillDetail {
	
	private FulfillmentLocation location;
	private OrderDetail detail;
	private Integer index;
	
	public OrderFillDetail() {
	}
	
	public OrderFillDetail(FulfillmentLocation location, OrderDetail detail, Integer index) {
		super();
		this.setLocation(location);
		this.setDetail(detail);
		this.index = index;
	}


	@PlanningVariable(valueRangeProviderRefs = "locationRange", nullable = true, strengthComparatorClass = FulfillmentLocationStrengthComparator.class)
	public FulfillmentLocation getLocation() {
		return location;
	}

	@JsonIgnore
	public Boolean hasLocation() {
		return location != null;
	}

	public void setLocation(FulfillmentLocation location) {
		if (location != null) {
			if (this.location != null) {
				this.location.getFulfillments().remove(this);
			}
			location.getFulfillments().add(this);
		}
		this.location = location;
	}

	public OrderDetail getDetail() {
		return detail;
	}

	public void setDetail(OrderDetail detail) {
		this.detail = detail;
		detail.getFulfillments().add(this);
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
			return String.valueOf(index);
	}

	@Override
	public int hashCode() {
		return Objects.hash(index);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OrderFillDetail other = (OrderFillDetail) obj;
		return Objects.equals(index, other.index);
	}
	
	@PlanningId
	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

}
