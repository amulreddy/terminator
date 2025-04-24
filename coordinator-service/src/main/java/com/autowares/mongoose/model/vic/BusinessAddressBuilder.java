package com.autowares.mongoose.model.vic;

import com.autowares.apis.ids.model.BusinessDetail;
import com.autowares.ipov3.common.BusinessAddress;

public class BusinessAddressBuilder {
	private BusinessAddress businessDetail = new BusinessAddress();

	public static BusinessAddressBuilder builder() {
		return new BusinessAddressBuilder();
	}

	public BusinessAddressBuilder withBusinessAddress(BusinessDetail address) {
		businessDetail.setBusinessName(address.getBusinessName());
		businessDetail.setAddressLine1(address.getAddress());
		businessDetail.setAddressLine2(address.getAddress2());
		businessDetail.setCity(address.getCity());
		businessDetail.setStateCode(address.getStateProv());
		businessDetail.setPostalCode(address.getPostalCode());
		businessDetail.setCountryCode("US");
		return this;
	}

	public BusinessAddressBuilder withAccountNumber(String accountNumber) {
		businessDetail.setAccountNumber(accountNumber);
		return this;
	}

	public BusinessAddress build() {
		return businessDetail;
	}
}
