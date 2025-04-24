package com.autowares.mongoose.optaplanner.domain;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class StockDetail {
	
	private Integer detailId;
	private Integer quantity;
	private List<StockPutawayDetail> putawayDetails = new ArrayList<>();

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public Integer getDetailId() {
		return detailId;
	}

	public void setDetailId(Integer detailId) {
		this.detailId = detailId;
	}

	@JsonIgnore
	public List<StockPutawayDetail> getPutawayDetails() {
		return putawayDetails;
	}

	public void setPutawayDetails(List<StockPutawayDetail> putawayDetails) {
		this.putawayDetails = putawayDetails;
	}

}
