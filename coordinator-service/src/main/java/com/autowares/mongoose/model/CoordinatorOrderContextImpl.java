package com.autowares.mongoose.model;

import java.time.ZonedDateTime;

import com.autowares.servicescommon.model.OrderSource;
import com.autowares.servicescommon.model.SourceDocumentType;

public class CoordinatorOrderContextImpl extends CoordinatorContextImpl implements CoordinatorOrderContext {

	private ZonedDateTime orderTime;
	private String xmlOrderId;
	private Boolean invalid = false;
	private Boolean psxToBeDelivered = false;
	private Long retailOrderId;

	public CoordinatorOrderContextImpl() {
		this.setSourceDocumentType(SourceDocumentType.PurchaseOrder);
	}

	public CoordinatorOrderContextImpl(CoordinatorContext context) {
		super(context);
		this.xmlOrderId = context.getDocumentId();
	}

	@Override
	public String getXmlOrderId() {
		return this.xmlOrderId;
	}

	@Override
	public String getPurchaseOrder() {
		return getCustomerDocumentId();
	}

	@Override
	public OrderSource getOrderSource() {
		return getInquiryOptions().getLookupSource();
	}

	@Override
	public Long getSourceOrderId() {
		return retailOrderId;
	}
	
	public void setSourceOrderId(Long retailOrderId) {
		this.retailOrderId = retailOrderId;
	}

	@Override
	public Boolean getPsxToBeDelivered() {
		return psxToBeDelivered;
	}

	@Override
	public Boolean getInvalid() {
		return invalid;
	}

	@Override
	public void setInvalid(Boolean invalid) {
		this.invalid = invalid;
	}

	@Override
	public ZonedDateTime getOrderTime() {
		return orderTime;
	}

	public void setOrderTime(ZonedDateTime orderTime) {
		this.orderTime = orderTime;
	}

	public void setXmlOrderId(String xmlOrderId) {
		this.xmlOrderId = xmlOrderId;
	}

	public void setPsxToBeDelivered(Boolean psxToBeDelivered) {
		this.psxToBeDelivered = psxToBeDelivered;
	}

    @Override
    public void setPurchaseOrder(String purchaseOrder) {
        this.setCustomerDocumentId(purchaseOrder);       
    }


}