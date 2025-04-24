package com.autowares.mongoose.model.purchaseorder;

public class GcommEDIDeliveryDetails implements DeliveryDetails {
	private SendPurchaseOrderMethod method = SendPurchaseOrderMethod.GCOMM_EDI;

	@Override
	public SendPurchaseOrderMethod getMethod() {
		return method;
	}

}
