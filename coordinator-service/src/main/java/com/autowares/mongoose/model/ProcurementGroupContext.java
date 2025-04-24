package com.autowares.mongoose.model;

import com.autowares.supplychain.model.ProcurementGroup;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ProcurementGroupContext extends ProcurementGroup {

	@JsonIgnore
	private TransactionalContext supplierContext;
	@JsonIgnore
	private TransactionalContext customerContext;
	private boolean isResolved = false;

	public ProcurementGroupContext(ProcurementGroup group) {
		super(group);
	}

	public TransactionalContext getSupplierContext() {
		return supplierContext;
	}

	public void setSupplierContext(TransactionalContext supplierContext) {
		this.supplierContext = supplierContext;
	}

	public TransactionalContext getCustomerContext() {
		return customerContext;
	}

	public void setCustomerContext(TransactionalContext customerContext) {
		this.customerContext = customerContext;
	}

	public boolean isResolved() {
		return isResolved;
	}

	public void setResolved(boolean isResolved) {
		this.isResolved = isResolved;
	}



}
