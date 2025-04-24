package com.autowares.mongoose.optaplanner.domain;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class OrderDetail {
	
	private Integer detailId;
	private Integer orderAmount;
	private List<OrderFillDetail> fulfillments = new ArrayList<>();

	public Integer getOrderAmount() {
		return orderAmount;
	}

	public void setOrderAmount(Integer orderAmount) {
		this.orderAmount = orderAmount;
	}

	public Integer getDetailId() {
		return detailId;
	}

	public void setDetailId(Integer detailId) {
		this.detailId = detailId;
	}

	@JsonIgnore
	public List<OrderFillDetail> getFulfillments() {
		return fulfillments;
	}

	public void setFulfillments(List<OrderFillDetail> fulfillments) {
		this.fulfillments = fulfillments;
	}

}
