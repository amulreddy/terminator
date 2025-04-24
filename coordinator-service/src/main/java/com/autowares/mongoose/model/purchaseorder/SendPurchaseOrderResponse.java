package com.autowares.mongoose.model.purchaseorder;

public class SendPurchaseOrderResponse {
	private String message;
	private String customerPurchaseOrder;
	private Boolean sent;
	private Boolean async;
	private String correlationId;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getCustomerPurchaseOrder() {
		return customerPurchaseOrder;
	}

	public void setCustomerPurchaseOrder(String customerPurchaseOrder) {
		this.customerPurchaseOrder = customerPurchaseOrder;
	}

	public Boolean isSent() {
		return sent;
	}

	public void setSent(Boolean sent) {
		this.sent = sent;
	}

	public Boolean isAsync() {
		return async;
	}

	public void setAsync(Boolean async) {
		this.async = async;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}

}
