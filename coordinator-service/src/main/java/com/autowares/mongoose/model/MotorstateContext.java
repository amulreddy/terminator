package com.autowares.mongoose.model;

import com.autowares.motorstateservice.model.MotorstatePurchaseOrder;

public class MotorstateContext {

	private CoordinatorOrderContext coordinatorOrderContext;
	private MotorstatePurchaseOrder motorstatePurchaseOrder;
//	private Boolean isValid = false;
	
	public MotorstateContext(CoordinatorOrderContext coordinatorOrderContext,
			MotorstatePurchaseOrder motorstatePurchaseOrder) {
		super();
		this.coordinatorOrderContext = coordinatorOrderContext;
		this.motorstatePurchaseOrder = motorstatePurchaseOrder;
	}

	public CoordinatorOrderContext getCoordinatorOrderContext() {
		return coordinatorOrderContext;
	}

	public void setCoordinatorOrderContext(CoordinatorOrderContext coordinatorOrderContext) {
		this.coordinatorOrderContext = coordinatorOrderContext;
	}

	public MotorstatePurchaseOrder getMotorstatePurchaseOrder() {
		return motorstatePurchaseOrder;
	}

	public void setMotorstatePurchaseOrder(MotorstatePurchaseOrder motorstatePurchaseOrder) {
		this.motorstatePurchaseOrder = motorstatePurchaseOrder;
	}
	
	
}
