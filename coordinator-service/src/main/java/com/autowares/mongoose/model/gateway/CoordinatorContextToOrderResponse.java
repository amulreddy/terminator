package com.autowares.mongoose.model.gateway;

import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.FulfillmentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.servicescommon.model.Freight;
import com.autowares.servicescommon.model.FulfillmentLocation;
import com.autowares.xmlgateway.model.InquiryResponse;
import com.autowares.xmlgateway.model.ResponseItem;

public class CoordinatorContextToOrderResponse {

	private InquiryResponse response = new InquiryResponse();

	public CoordinatorContextToOrderResponse(CoordinatorOrderContext context) {
		for (FulfillmentLocationContext fulfillment : context.getFulfillmentSequence()) {
			if (fulfillment.isBeingFilledFrom()) {
				Freight fulfillmentLocation = new Freight();
				fulfillmentLocation.setLocationName(fulfillment.getLocation());
				fulfillmentLocation.setEstimatedDeliveryDateTime(fulfillment.getArrivalDate());
				fulfillmentLocation.setEstimatedShipDateTime(fulfillment.getNextDeparture());
				for (FulfillmentContext fillDetail : fulfillment.getFulfillmentDetails()) {
					if (fillDetail.getFillQuantity() > 0) {
						LineItemContext orderDetail = fillDetail.getLineItem();
						ResponseItem responseItem = convertLineItemContext(orderDetail);
						responseItem.setPlannedFillQuantity(fillDetail.getFillQuantity());
						responseItem.setAvailableQuantity(fillDetail.getQuantityOnHand());
						responseItem.getFulfillmentPlan().add(fulfillmentLocation);
					}
				}
			}
		}
		boolean addNonfillable = false;
		FulfillmentLocation nonfillable = new FulfillmentLocation();
		nonfillable.setLocationName("Not fillable");
		for (LineItemContext orderDetail : context.getLineItems()) {
			if (orderDetail.getInvalid() || orderDetail.getFulfillmentDetails().isEmpty() ||orderDetail.getShortageCode() !=null) {
				addNonfillable = true;
				ResponseItem responseItem = convertLineItemContext(orderDetail);
				responseItem.setPlannedFillQuantity(0);
			}
		}
		if (addNonfillable) {
		}
	}

	public ResponseItem convertLineItemContext(LineItemContext lineContext) {
		ResponseItem responseItem = new ResponseItem();
		responseItem.setProductId(lineContext.getProductId());
		responseItem.setBrandAaiaId(lineContext.getBrandAaiaId());
		responseItem.setCounterworksLineCode(lineContext.getCounterWorksLineCode());
		responseItem.setLineCode(lineContext.getLineCode());
		if (lineContext.getCustomerLineNumber() != null) {
			responseItem.setLineNumber(lineContext.getCustomerLineNumber().intValue());
		} else {
			responseItem.setLineNumber(lineContext.getLineNumber());
		}
		responseItem.setPartNumber(lineContext.getPartNumber());
		responseItem.setPrice(lineContext.getPrice());
		responseItem.setQuantity(lineContext.getQuantity());
		responseItem.setVendorCodeSubCode(lineContext.getVendorCodeSubCode());
		if (lineContext.getShortageCode() != null) {
			responseItem.setShortageCode(lineContext.getShortageCode().toString());
		}
		return responseItem;
	}

	public InquiryResponse get() {
		return this.response;
	}

}
