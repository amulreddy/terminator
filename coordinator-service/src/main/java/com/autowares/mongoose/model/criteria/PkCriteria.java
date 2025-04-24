package com.autowares.mongoose.model.criteria;

public class PkCriteria {

	private String vendorCode;
	private String buildingCode;
	private Boolean consolidationEnabled;
	
	public String getVendorCode() {
		return vendorCode;
	}
	public void setVendorCode(String vendorCode) {
		this.vendorCode = vendorCode;
	}
	public String getBuildingCode() {
		return buildingCode;
	}
	public void setBuildingCode(String buildingCode) {
		this.buildingCode = buildingCode;
	}
	public Boolean getConsolidationEnabled() {
		return consolidationEnabled;
	}
	public void setConsolidationEnabled(Boolean consolidationEnabled) {
		this.consolidationEnabled = consolidationEnabled;
	}
	
}
