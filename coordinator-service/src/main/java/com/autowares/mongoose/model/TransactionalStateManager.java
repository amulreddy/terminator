package com.autowares.mongoose.model;

import com.autowares.servicescommon.client.BaseResillience4JClient;
import com.autowares.servicescommon.model.LineItem;
import com.autowares.servicescommon.model.SourceDocument;
import com.autowares.supplychain.model.ProcurementGroup;

public interface TransactionalStateManager {
	
	CoordinatorContext resolveOrderContext(String documentId);
	SourceDocument retrieve(String documentId);
	SourceDocument persist(CoordinatorContext context);
	SourceDocument persist(SourceDocument document);
	BaseResillience4JClient getClient();
	<T extends LineItem> T persist(T lineItem);
	TransactionalContext resolveTransactionalContext(String documentId);
	ProcurementGroupContext resolveProcurementGroupContext(ProcurementGroup pg);
	SourceDocument retrieve(Long supplyChainId);
	CoordinatorContext resolveOrderContext(Long supplyChainId);
	
}
