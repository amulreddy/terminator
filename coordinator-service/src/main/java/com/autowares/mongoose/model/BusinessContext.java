package com.autowares.mongoose.model;

import com.autowares.apis.ids.model.BusinessDetail;
import com.autowares.servicescommon.model.BusinessLocation;
import com.autowares.servicescommon.model.BusinessLocationAccount;
import com.autowares.servicescommon.model.BusinessLocationImpl;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.supplychain.model.SupplyChainBusiness;
import com.autowares.supplychain.model.SupplyChainVendor;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class BusinessContext implements BusinessLocationAccount {

	private BusinessDetail businessDetail = new BusinessDetail();
	@JsonIgnore
	private PhysicalContext physicalContext;
	private Shop shop;

	public BusinessContext() {
	}

	public BusinessContext(BusinessLocationAccount account) {
		this(account.getMember());
		businessDetail.setAwiAccountNo(account.getAccountNumber());
	}

	public BusinessContext(BusinessLocation location) {
		if (location != null) {
			if (location.getBusinessId() != null) {
				businessDetail.setBusEntId(location.getBusinessId());
			}
			businessDetail.setAddress(location.getAddress());
			businessDetail.setAddress2(location.getAddress2());
			businessDetail.setCity(location.getCity());
			businessDetail.setStateProv(location.getStateProv());
			businessDetail.setPostalCode(location.getPostalCode());
			businessDetail.setBusinessName(location.getName());
			if(location instanceof SupplyChainVendor) {
				SupplyChainVendor vendor = (SupplyChainVendor) location;
				businessDetail.setGreatPlainsVendorId(vendor.getGreatPlainsVendorId());
			}
		}
	}
	
	public BusinessContext(SupplyChainBusiness location) {
		this((BusinessLocation) location);
		businessDetail.setAwiAccountNo(location.getAccountNumber());
	}
	
	

	@Override
	public BusinessLocation getMember() {
		if (businessDetail != null) {
			BusinessLocationImpl entity = new BusinessLocationImpl();
			entity.setId(businessDetail.getBusEntId());
			entity.setName(businessDetail.getBusinessName());
			entity.setAddress(businessDetail.getAddress());
			entity.setAddress2(businessDetail.getAddress2());
			entity.setBusinessId(businessDetail.getBusEntId());
			entity.setCity(businessDetail.getCity());
			entity.setPostalCode(businessDetail.getPostalCode());
			entity.setStateProv(businessDetail.getStateProv());
			return entity;
		}
		return null;
	}

	@Override
	public String getAccountNumber() {
		if (businessDetail != null) {
			return businessDetail.getAwiAccountNo();
		}
		return null;
	}

	@Override
	public SystemType getSystemType() {
		return SystemType.AwiWarehouse;
	}

	@JsonIgnore
	public BusinessDetail getBusinessDetail() {
		return businessDetail;
	}

	public void setBusinessDetail(BusinessDetail businessDetail) {
		this.businessDetail = businessDetail;
	}

	public PhysicalContext getPhysicalContext() {
		return physicalContext;
	}

	public void setPhysicalContext(PhysicalContext physicalContext) {
		this.physicalContext = physicalContext;
	}

	public Shop getShop() {
		return shop;
	}

	public void setShop(Shop shop) {
		this.shop = shop;
	}

}
