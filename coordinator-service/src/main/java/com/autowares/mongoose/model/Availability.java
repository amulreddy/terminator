package com.autowares.mongoose.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.autowares.apis.partservice.PartAvailability;
import com.autowares.orders.model.Item;
import com.autowares.servicescommon.model.HandlingPriorityType;
import com.autowares.supplychain.model.OperationalContext;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class Availability {

    @JsonIgnore
    private LineItemContext lineItem;
    private FulfillmentLocationContext fulfillmentLocation;
    private PartAvailability partAvailability;
    private BigDecimal procurementCost;
    private int quantityOnHand;
    private int fillQuantity;
    private List<FulfillmentContext> fulfillments = new CopyOnWriteArrayList<>();
    @JsonIgnore
    private LineItemContext matchingLineItem;
    private Item moaOrderDetail;
    private HandlingPriorityType handlingPriorityType;
    private OperationalContext operationalContext;

    public Availability(LineItemContext lineItem, FulfillmentLocationContext fulfillmentLocation,
            LineItemContext matchingLineItem) {
        this.lineItem = lineItem;
        lineItem.getAvailability().add(this);
        this.fulfillmentLocation = fulfillmentLocation;
        fulfillmentLocation.getLineItemAvailability().add(this);
        this.matchingLineItem = matchingLineItem;
    }

    public Availability(LineItemContext lineItem, FulfillmentLocationContext fulfillmentLocation) {
        this(lineItem, fulfillmentLocation, null);
    }

    public LineItemContext getLineItem() {
        return lineItem;
    }

    public void setLineItem(LineItemContext lineItem) {
        this.lineItem = lineItem;
    }

    public FulfillmentLocationContext getFulfillmentLocation() {
        return fulfillmentLocation;
    }

    public void setFulfillmentLocation(FulfillmentLocationContext fulfillmentLocation) {
        this.fulfillmentLocation = fulfillmentLocation;
    }

    public PartAvailability getPartAvailability() {
        return partAvailability;
    }

    public void setPartAvailability(PartAvailability partAvailability) {
        this.partAvailability = partAvailability;
    }

    public BigDecimal getProcurementCost() {
        return procurementCost;
    }

    public void setProcurementCost(BigDecimal procurementCost) {
        this.procurementCost = procurementCost;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(int quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public List<FulfillmentContext> getFulfillments() {
        return fulfillments;
    }

    public void setFulfillments(List<FulfillmentContext> fulfillments) {
        this.fulfillments = fulfillments;
    }

    public int getFillQuantity() {
        return fillQuantity;
    }

    public void setFillQuantity(int fillQuantity) {
        this.fillQuantity = fillQuantity;
    }

    public LineItemContext getMatchingLineItem() {
        return matchingLineItem;
    }

    public void setMatchingLineItem(LineItemContext matchingLineItem) {
        this.matchingLineItem = matchingLineItem;
    }

    public Item getMoaOrderDetail() {
        return moaOrderDetail;
    }

    public void setMoaOrderDetail(Item moaOrderDetail) {
        this.moaOrderDetail = moaOrderDetail;
    }

    public Integer getNumberOfTransfers() {
        if (fulfillmentLocation != null) {
            return fulfillmentLocation.getTransfers();
        }
        return null;
    }

    public HandlingPriorityType getHandlingPriorityType() {
        return handlingPriorityType;
    }

    public void setHandlingPriorityType(HandlingPriorityType handlingPriorityType) {
        this.handlingPriorityType = handlingPriorityType;
    }

	public OperationalContext getOperationalContext() {
		return operationalContext;
	}

	public void setOperationalContext(OperationalContext operationalContext) {
		this.operationalContext = operationalContext;
	}

}
