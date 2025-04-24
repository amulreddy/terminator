package com.autowares.mongoose.model.gateway;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoiceTransactionEvent {
	private String truckRun;
	private Integer lineNumber;
	private Double wdTrueCost;
	private String GComReceiverID;
	private String debitCredit;
	private Double jobberPrice;
	private String mustGo;
	private Double billPrice;
	private Long orderID;
	private String vendorName;
	private Double wdInvoiceCost;
	private Double priceExtended;
	private Long orderQuantity;
	private String partDescription;
	private Double wdInvoiceCostExtended;
	private String zone;
	private Integer shipQuantity;
	private Date invoiceDate;
	private String customerNumber;
	private String invoiceNumber;
	private String employeeNumber;
	private String buildingCode;
	private Long julianDate;
	private String progSource;
	private String transactionType;
	private Double wdTrueCostExtended;
	private String employeeName;
	private String shipperNumber;
	private String eventType;
	private Double coreValue;
	private String partNumber;
	private Date timeStamp;
	private String partClass;
	private String purchaseOrder;
	private Long warehouseNumber;
	private String pullerID;
	private Double wdCostExtended;
	private String warehouseName;
	private Long invoiceID;
	private String unitOfMeasure;
	private String vendor;
	private Double wdCost;
	private Date orderTime;

	public String getTruckRun() {
		return truckRun;
	}

	public void setTruckRun(String truckRun) {
		this.truckRun = truckRun;
	}

	public Integer getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(Integer lineNumber) {
		this.lineNumber = lineNumber;
	}

	public Double getWdTrueCost() {
		return wdTrueCost;
	}

	public void setWdTrueCost(Double wdTrueCost) {
		this.wdTrueCost = wdTrueCost;
	}

	public String getGComReceiverID() {
		return GComReceiverID;
	}

	public void setGComReceiverID(String gComReceiverID) {
		GComReceiverID = gComReceiverID;
	}

	public String getDebitCredit() {
		return debitCredit;
	}

	public void setDebitCredit(String debitCredit) {
		this.debitCredit = debitCredit;
	}

	public Double getJobberPrice() {
		return jobberPrice;
	}

	public void setJobberPrice(Double jobberPrice) {
		this.jobberPrice = jobberPrice;
	}

	public String getMustGo() {
		return mustGo;
	}

	public void setMustGo(String mustGo) {
		this.mustGo = mustGo;
	}

	public Double getBillPrice() {
		return billPrice;
	}

	public void setBillPrice(Double billPrice) {
		this.billPrice = billPrice;
	}

	public Long getOrderID() {
		return orderID;
	}

	public void setOrderID(Long orderID) {
		this.orderID = orderID;
	}

	public String getVendorName() {
		return vendorName;
	}

	public void setVendorName(String vendorName) {
		this.vendorName = vendorName;
	}

	public Double getWdInvoiceCost() {
		return wdInvoiceCost;
	}

	public void setWdInvoiceCost(Double wdInvoiceCost) {
		this.wdInvoiceCost = wdInvoiceCost;
	}

	public Double getPriceExtended() {
		return priceExtended;
	}

	public void setPriceExtended(Double priceExtended) {
		this.priceExtended = priceExtended;
	}

	public Long getOrderQuantity() {
		return orderQuantity;
	}

	public void setOrderQuantity(Long orderQuantity) {
		this.orderQuantity = orderQuantity;
	}

	public String getPartDescription() {
		return partDescription;
	}

	public void setPartDescription(String partDescription) {
		this.partDescription = partDescription;
	}

	public Double getWdInvoiceCostExtended() {
		return wdInvoiceCostExtended;
	}

	public void setWdInvoiceCostExtended(Double wdInvoiceCostExtended) {
		this.wdInvoiceCostExtended = wdInvoiceCostExtended;
	}

	public String getZone() {
		return zone;
	}

	public void setZone(String zone) {
		this.zone = zone;
	}

	public Integer getShipQuantity() {
		return shipQuantity;
	}

	public void setShipQuantity(Integer shipQuantity) {
		this.shipQuantity = shipQuantity;
	}

	public Date getInvoiceDate() {
		return invoiceDate;
	}

	public void setInvoiceDate(Date invoiceDate) {
		this.invoiceDate = invoiceDate;
	}

	public String getCustomerNumber() {
		return customerNumber;
	}

	public void setCustomerNumber(String customerNumber) {
		this.customerNumber = customerNumber;
	}

	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	public void setInvoiceNumber(String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	public String getEmployeeNumber() {
		return employeeNumber;
	}

	public void setEmployeeNumber(String employeeNumber) {
		this.employeeNumber = employeeNumber;
	}

	public String getBuildingCode() {
		return buildingCode;
	}

	public void setBuildingCode(String buildingCode) {
		this.buildingCode = buildingCode;
	}

	public Long getJulianDate() {
		return julianDate;
	}

	public void setJulianDate(Long julianDate) {
		this.julianDate = julianDate;
	}

	public String getProgSource() {
		return progSource;
	}

	public void setProgSource(String progSource) {
		this.progSource = progSource;
	}

	public String getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}

	public Double getWdTrueCostExtended() {
		return wdTrueCostExtended;
	}

	public void setWdTrueCostExtended(Double wdTrueCostExtended) {
		this.wdTrueCostExtended = wdTrueCostExtended;
	}

	public String getEmployeeName() {
		return employeeName;
	}

	public void setEmployeeName(String employeeName) {
		this.employeeName = employeeName;
	}

	public String getShipperNumber() {
		return shipperNumber;
	}

	public void setShipperNumber(String shipperNumber) {
		this.shipperNumber = shipperNumber;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public Double getCoreValue() {
		return coreValue;
	}

	public void setCoreValue(Double coreValue) {
		this.coreValue = coreValue;
	}

	public String getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getPartClass() {
		return partClass;
	}

	public void setPartClass(String partClass) {
		this.partClass = partClass;
	}

	public String getPurchaseOrder() {
		return purchaseOrder;
	}

	public void setPurchaseOrder(String purchaseOrder) {
		this.purchaseOrder = purchaseOrder;
	}

	public Long getWarehouseNumber() {
		return warehouseNumber;
	}

	public void setWarehouseNumber(Long warehouseNumber) {
		this.warehouseNumber = warehouseNumber;
	}

	public String getPullerID() {
		return pullerID;
	}

	public void setPullerID(String pullerID) {
		this.pullerID = pullerID;
	}

	public Double getWdCostExtended() {
		return wdCostExtended;
	}

	public void setWdCostExtended(Double wdCostExtended) {
		this.wdCostExtended = wdCostExtended;
	}

	public String getWarehouseName() {
		return warehouseName;
	}

	public void setWarehouseName(String warehouseName) {
		this.warehouseName = warehouseName;
	}

	public Long getInvoiceID() {
		return invoiceID;
	}

	public void setInvoiceID(Long invoiceID) {
		this.invoiceID = invoiceID;
	}

	public String getUnitOfMeasure() {
		return unitOfMeasure;
	}

	public void setUnitOfMeasure(String unitOfMeasure) {
		this.unitOfMeasure = unitOfMeasure;
	}

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public Double getWdCost() {
		return wdCost;
	}

	public void setWdCost(Double wdCost) {
		this.wdCost = wdCost;
	}

	public Date getOrderTime() {
		return orderTime;
	}

	public void setOrderTime(Date orderTime) {
		this.orderTime = orderTime;
	}

}
