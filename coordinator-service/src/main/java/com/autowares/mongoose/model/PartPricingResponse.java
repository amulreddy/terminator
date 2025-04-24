package com.autowares.mongoose.model;

public class PartPricingResponse {
	private Double price;
	private Long partHeaderId;
	public Double getPrice() {
		return price;
	}
	public void setPrice(Double price) {
		this.price = price;
	}
	public Long getPartHeaderId() {
		return partHeaderId;
	}
	public void setPartHeaderId(Long partHeaderId) {
		this.partHeaderId = partHeaderId;
	}
	
}
