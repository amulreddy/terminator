package com.autowares.mongoose.model.purchaseorder;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "deliveryType", visible = true)
@JsonSubTypes({ @Type(value = EmailDeliveryDetails.class, name = "EMAIL"),
		@Type(value = GcommEDIDeliveryDetails.class, name = "GCOMM_EDI"),
		@Type(value = IPODeliveryDetails.class, name = "IPO") })
public interface DeliveryDetails {
	SendPurchaseOrderMethod getMethod();
}
