package com.autowares.mongoose.model;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.autowares.servicescommon.model.FreightOptions;
import com.autowares.servicescommon.model.ShortageCode;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class FulfillmentContext {

	private FulfillmentLocationContext fulfillmentLocation;
	@JsonIgnore
	private LineItemContext lineItem;
	private Boolean partial = false;
	private Integer quantityOnHand = 0;
	private Double scoreValue = 0.0;
	private Integer fillQuantity = 0;
	private Boolean store = false;
	private OrderHandling orderHandling = OrderHandling.Pick;
	private BigDecimal procurementCost;
	private FreightOptions freightOptions;
	private Boolean invalid;
	private ShortageCode notFillabelCode; // new enum - a reason for why this is invalid.
	private Availability availability;

	public FulfillmentContext() {
	}

	public FulfillmentContext(LineItemContext lineItem) {
		this.lineItem = lineItem;
		lineItem.getFulfillmentDetails().add(this);
	}

	public String getLocation() {
		if (fulfillmentLocation != null) {
			return fulfillmentLocation.getLocation();
		}
		return null;
	}

	public Long getDaysToDeliver() {
		if (fulfillmentLocation != null) {
			return fulfillmentLocation.getDaysToDeliver();
		}
		return null;
	}

	public Integer getTransfers() {
		if (fulfillmentLocation != null) {
			return fulfillmentLocation.getTransfers();
		}
		return null;
	}

	public Double getLogisticalScore() {
		if (fulfillmentLocation != null) {
			return fulfillmentLocation.getScore();
		}
		return null;
	}

	public ZonedDateTime getArrivalDate() {
		if (fulfillmentLocation != null) {
			return fulfillmentLocation.getArrivalDate();
		}
		return null;
	}

	public FulfillmentLocationContext getFulfillmentLocation() {
		return fulfillmentLocation;
	}

	public void setFulfillmentLocation(FulfillmentLocationContext fulfillmentLocation) {
		// if this fulfillment was associated with a different fulfillment location, remove it from the previous one
		if(this.getFulfillmentLocation() != null) {
			this.getFulfillmentLocation().getFulfillmentDetails().remove(this);
		}
		this.fulfillmentLocation = fulfillmentLocation;
		// add this fulfillment to the new fulfillment location
		fulfillmentLocation.getFulfillmentDetails().add(this);
	}

	public LineItemContext getLineItem() {
		return lineItem;
	}

	public void setLineItem(LineItemContext orderDetailContext) {
		this.lineItem = orderDetailContext;
	}

	public Boolean getPartial() {
		return partial;
	}

	public void setPartial(Boolean partial) {
		this.partial = partial;
	}

	public Integer getQuantityOnHand() {
		return quantityOnHand;
	}

	public void setQuantityOnHand(Integer quantityOnHand) {
		this.quantityOnHand = quantityOnHand;
	}

	public Double getScoreValue() {
		return scoreValue;
	}

	public void setScoreValue(Double scoreValue) {
		this.scoreValue = scoreValue;
	}

	public Integer getFillQuantity() {
		return fillQuantity;
	}

	public void setFillQuantity(Integer fillQuantity) {
		this.fillQuantity = fillQuantity;
	}

	public Boolean getStore() {
		return store;
	}

	public void setStore(Boolean store) {
		this.store = store;
	}

	public OrderHandling getOrderHandling() {
		return orderHandling;
	}

	public void setOrderHandling(OrderHandling orderHandling) {
		this.orderHandling = orderHandling;
	}

	public BigDecimal getProcurementCost() {
		return procurementCost;
	}

	public void setProcurementCost(BigDecimal procurementCost) {
		this.procurementCost = procurementCost;
	}

	public FreightOptions getFreightOptions() {
		return freightOptions;
	}

	public void setFreightOptions(FreightOptions freightOptions) {
		this.freightOptions = freightOptions;
	}

	public Boolean getInvalid() {
		return invalid;
	}

	public void setInvalid(Boolean invalid) {
		this.invalid = invalid;
	}

	public ShortageCode getNotFillabelCode() {
		return notFillabelCode;
	}

	public void setNotFillabelCode(ShortageCode notFillabelCode) {
		this.notFillabelCode = notFillabelCode;
	}

	public Availability getAvailability() {
		return availability;
	}

	public void setAvailability(Availability availability) {
		this.availability = availability;
	}

}
