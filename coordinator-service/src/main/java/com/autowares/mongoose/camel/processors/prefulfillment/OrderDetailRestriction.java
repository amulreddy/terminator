package com.autowares.mongoose.camel.processors.prefulfillment;

import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.purchaserestrictions.client.PurchaseRestrictionClient;
import com.autowares.purchaserestrictions.model.PurchaseRestriction;
import com.autowares.purchaserestrictions.model.PurchaseRestrictionLineItem;
import com.autowares.servicescommon.model.ShortageCode;

@Component
public class OrderDetailRestriction implements Processor {

	PurchaseRestrictionClient purchaseRestrictionClient = new PurchaseRestrictionClient();

	@Override
	public void process(Exchange exchange) throws Exception {
		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);

		PurchaseRestriction restrictionRequest = new PurchaseRestriction();
		// log.info("Checking order restrictions: " + context.getXmlOrderId() + " " + "
		// source: "
//				+ context.getOrderSource());
		restrictionRequest.setAccountNumber(String.valueOf(context.getCustomerNumber()));
		restrictionRequest.setDocumentId(context.getDocumentId());
		restrictionRequest
				.setLineItems(context.getLineItems().stream().map(i -> new PurchaseRestrictionLineItem(i)).collect(Collectors.toList()));
		restrictionRequest = purchaseRestrictionClient.checkRestrictions(restrictionRequest);

		for (LineItemContext detail : context.getLineItems()) {
			for (PurchaseRestrictionLineItem lineItem : restrictionRequest.getLineItems()) {
				if (lineItem.getLineNumber().equals(detail.getLineNumber())) {
					if (lineItem.isRestricted()) {
						detail.setInvalid(true);
						detail.setShortageCode(ShortageCode.RestrictedFromPurchase);
						context.updateProcessingLog(lineItem.getRejectionReason() + ".  Line Number : "
								+ detail.getLineNumber() + " Part Number : " + detail.getPartNumber() + "  Vendor = "
								+ detail.getVendorCodeSubCode());
					}
				}
			}
		}
	}

}
