package com.autowares.mongoose.events;

public class ViperOrderUpdateEvent extends BaseViperEvent {
	
	private Integer orderCustomerRec;
	private Integer shipQuantity;
	private String status;

	public Integer getOrderCustomerRec() {
		return orderCustomerRec;
	}

	public void setOrderCustomerRec(Integer orderCustomerRec) {
		this.orderCustomerRec = orderCustomerRec;
	}

	public Integer getShipQuantity() {
		return shipQuantity;
	}

	public void setShipQuantity(Integer shipQuantity) {
		this.shipQuantity = shipQuantity;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
