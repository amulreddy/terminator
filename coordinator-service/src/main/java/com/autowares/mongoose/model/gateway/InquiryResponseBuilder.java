package com.autowares.mongoose.model.gateway;

import java.util.List;

import com.autowares.servicescommon.model.BusinessLocationAccount;
import com.autowares.xmlgateway.model.InquiryResponse;
import com.autowares.xmlgateway.model.ResponseItem;

public class InquiryResponseBuilder {
	
	private InquiryResponse response = new InquiryResponse();
	
	public InquiryResponseBuilder fromResponse(InquiryResponse response) {
		this.response = response;
		return this;
	}
	
	public InquiryResponseBuilder withResponseItems(List<ResponseItem> items) {
		this.response.setLineItems(items);
		return this;
	}
	
	public InquiryResponseBuilder withResponseItem(ResponseItem item) {
		this.response.getLineItems().add(item);
		return this;
	}
	
	public InquiryResponseBuilder withDocumentReferenceId(String documentReferenceId) {
		this.response.setDocumentReferenceId(documentReferenceId);
		return this;
	}
	
	public InquiryResponseBuilder withShipTo(BusinessLocationAccount shipTo) {
		this.response.setShipTo(shipTo);
		return this;
	}
	
	public InquiryResponse build() {
		return response;
	}
}
