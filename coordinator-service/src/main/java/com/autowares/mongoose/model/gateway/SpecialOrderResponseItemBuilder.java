package com.autowares.mongoose.model.gateway;

import com.autowares.servicescommon.model.SystemType;
import com.autowares.xmlgateway.model.ResponseItemBuilder;

public class SpecialOrderResponseItemBuilder extends ResponseItemBuilder<SpecialOrderResponseItem> {
	
	public SpecialOrderResponseItemBuilder() {
		super.item = new SpecialOrderResponseItem();
	}
	

	public ResponseItemBuilder<SpecialOrderResponseItem> withSpecialOrderSystem(SystemType systemType) {
		if (!this.item.getSpecialOrderSystems().contains(systemType)) {
			this.item.getSpecialOrderSystems().add(systemType);
		}
		this.item.setCanSpecialOrder(true);
		return this;
	}

}
