package com.autowares.mongoose.model;

import com.autowares.servicescommon.model.OperationalItem;
import com.autowares.supplychain.model.OperationalContext;

public interface OperationalStateManager {

	OperationalContext getOperationalContext(Long itemId);
	OperationalContext getOperationalContext(String xmlOrderId, String building, Integer lineNumber);
	OperationalContext getOperationalContext(String packSlipId, Integer lineNumber);
	OperationalContext persist(OperationalContext context);
	<T extends OperationalItem> T persist(T operationalItem);
	void mergeContexts(OperationalContext sourceContext, OperationalContext targetContext);
}
