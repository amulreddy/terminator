package com.autowares.mongoose.model.purchaseorder;

public class EmailDeliveryDetails implements DeliveryDetails {

	private SendPurchaseOrderMethod method = SendPurchaseOrderMethod.EMAIL;
	private String fromEmailAddress;
	private String toEmailAddress;
	private String ccEmailAddress;
	
	@Override
	public SendPurchaseOrderMethod getMethod() {
		return method;
	}

	public String getFromEmailAddress() {
		return fromEmailAddress;
	}

	public void setFromEmailAddress(String fromEmailAddress) {
		this.fromEmailAddress = fromEmailAddress;
	}

	public String getToEmailAddress() {
		return toEmailAddress;
	}

	public void setToEmailAddress(String toEmailAddress) {
		this.toEmailAddress = toEmailAddress;
	}

	public String getCcEmailAddress() {
		return ccEmailAddress;
	}

	public void setCcEmailAddress(String ccEmailAddress) {
		this.ccEmailAddress = ccEmailAddress;
	}

	public void setMethod(SendPurchaseOrderMethod method) {
		this.method = method;
	}

}
