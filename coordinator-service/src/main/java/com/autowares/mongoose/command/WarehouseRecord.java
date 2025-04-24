package com.autowares.mongoose.command;

public class WarehouseRecord {
	private Short warehouseNumber;
	private String warehouseName;
	private String warehouseBusinessName;
	private String buildingMnemonic;

	public Short getWarehouseNumber() {
		return warehouseNumber;
	}

	public void setWarehouseNumber(Short warehouseNumber) {
		this.warehouseNumber = warehouseNumber;
	}

	public String getWarehouseName() {
		return warehouseName;
	}

	public void setWarehouseName(String warehouseName) {
		this.warehouseName = warehouseName;
	}

	public String getWarehouseBusinessName() {
		return warehouseBusinessName;
	}

	public void setWarehouseBusinessName(String warehouseBusinessName) {
		this.warehouseBusinessName = warehouseBusinessName;
	}

	public String getBuildingMnemonic() {
		return buildingMnemonic;
	}

	public void setBuildingMnemonic(String buildingMnemonic) {
		this.buildingMnemonic = buildingMnemonic;
	}

}
