package com.autowares.mongoose.model;

import java.time.ZonedDateTime;

import com.autowares.servicescommon.model.Account;
import com.autowares.servicescommon.model.Order;
import com.autowares.servicescommon.model.OrderSource;
import com.autowares.servicescommon.model.Party;
import com.autowares.servicescommon.model.PurchaseOrderType;
import com.autowares.servicescommon.model.SystemType;

public interface CoordinatorOrderContext extends Order, CoordinatorContext  {
	
	String getXmlOrderId();
	
	String getPurchaseOrder();
	
	OrderSource getOrderSource();

	Long getSourceOrderId();
	
	Boolean getPsxToBeDelivered();
	
	Boolean getInvalid();

	void setInvalid(Boolean invalid);
	
	ZonedDateTime getOrderTime();
	
	PurchaseOrderType getOrderType();
	void setOrderType(PurchaseOrderType orderType);

	@Override
	default Account getBuyingAccount() {
		return CoordinatorContext.super.getBuyingAccount();
	}

	@Override
	default Party getSellingParty() {
		return CoordinatorContext.super.getSellingParty();
	}

    void setPurchaseOrder(String purchaseOrder);

	@Override
	SystemType getSystemType();
	
}
