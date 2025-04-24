package com.autowares.mongoose.model.gateway;

import java.util.ArrayList;
import java.util.List;

import com.autowares.servicescommon.model.SystemType;
import com.autowares.xmlgateway.model.ResponseItem;

public class SpecialOrderResponseItem extends ResponseItem {
	private Boolean canSpecialOrder = false;
	private List<SystemType> specialOrderSystems = new ArrayList<>();

	public Boolean getCanSpecialOrder() {
		return canSpecialOrder;
	}

	public void setCanSpecialOrder(Boolean canSpecialOrder) {
		this.canSpecialOrder = canSpecialOrder;
	}

	public List<SystemType> getSpecialOrderSystems() {
		return specialOrderSystems;
	}

	public void setSpecialOrderSystems(List<SystemType> specialOrderSystems) {
		this.specialOrderSystems = specialOrderSystems;
	}

}
