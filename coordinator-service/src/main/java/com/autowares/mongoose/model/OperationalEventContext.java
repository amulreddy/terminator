package com.autowares.mongoose.model;

public class OperationalEventContext {
	private OperationalEventType operationalEventType;
	private Integer quantity;
	private IntegrationContext integrationContext;
	private Long moaOrderDetailId;
	private LineItemContext lineItemContext;
	private String toteSerial;
	private String rackLocation;
	private Boolean returnProcessing = false;
	private String putawayFile;	

	public OperationalEventContext() {
	}

	public OperationalEventContext(OperationalEventContext eventContext) {
		this.operationalEventType = eventContext.getOperationalEventType();
		this.quantity = eventContext.getQuantity();
		this.integrationContext = eventContext.getIntegrationContext();
		this.moaOrderDetailId = eventContext.getMoaOrderDetailId();
		this.lineItemContext = eventContext.getLineItemContext();
		this.toteSerial = eventContext.getToteSerial();
		this.rackLocation = eventContext.getRackLocation();
		this.returnProcessing = eventContext.getReturnProcessing();
		this.putawayFile = eventContext.getPutawayFile();
	}

	public OperationalEventType getOperationalEventType() {
		return operationalEventType;
	}

	public void setOperationalEventType(OperationalEventType operationalEventType) {
		this.operationalEventType = operationalEventType;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public IntegrationContext getIntegrationContext() {
		return integrationContext;
	}

	public void setIntegrationContext(IntegrationContext integrationContext) {
		this.integrationContext = integrationContext;
	}

	public Long getMoaOrderDetailId() {
		return moaOrderDetailId;
	}

	public void setMoaOrderDetailId(Long moaOrderDetailId) {
		this.moaOrderDetailId = moaOrderDetailId;
	}

	public LineItemContext getLineItemContext() {
		return lineItemContext;
	}

	public void setLineItemContext(LineItemContext lineItemContext) {
		this.lineItemContext = lineItemContext;
	}

	
	public String getToteSerial() {
		return toteSerial;
	}

	public void setToteSerial(String toteSerial) {
		this.toteSerial = toteSerial;
	}

	public String getRackLocation() {
		return rackLocation;
	}

	public void setRackLocation(String rackLocation) {
		this.rackLocation = rackLocation;
	}

	public Boolean getReturnProcessing() {
		return returnProcessing;
	}

	public void setReturnProcessing(Boolean returnProcessing) {
		this.returnProcessing = returnProcessing;
	}

	public String getPutawayFile() {
		return putawayFile;
	}

	public void setPutawayFile(String putawayFile) {
		this.putawayFile = putawayFile;
	}
}
