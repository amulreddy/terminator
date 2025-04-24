package com.autowares.mongoose.model.purchaseorder;

public class IPODeliveryDetails implements DeliveryDetails {
	private SendPurchaseOrderMethod method = SendPurchaseOrderMethod.IPO;

	@Override
	public SendPurchaseOrderMethod getMethod() {
		return method;
	}

}
