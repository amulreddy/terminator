package com.autowares.mongoose.model.purchaseorder;

public class SendPurchaseOrderRequest {
	private String customerPurchaseOrder;
	private Long supplyChainPurchaseOrderId;
	private SendPurchaseOrderMethod method;

	public String getCustomerPurchaseOrder() {
		return customerPurchaseOrder;
	}

	public void setCustomerPurchaseOrder(String customerPurchaseOrder) {
		this.customerPurchaseOrder = customerPurchaseOrder;
	}

	public Long getSupplyChainPurchaseOrderId() {
		return supplyChainPurchaseOrderId;
	}

	public void setSupplyChainPurchaseOrderId(Long supplyChainPurchaseOrderId) {
		this.supplyChainPurchaseOrderId = supplyChainPurchaseOrderId;
	}

	public SendPurchaseOrderMethod getMethod() {
		return method;
	}

	public void setMethod(SendPurchaseOrderMethod method) {
		this.method = method;
	}

}
