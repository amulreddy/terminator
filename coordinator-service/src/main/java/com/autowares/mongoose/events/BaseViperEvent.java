package com.autowares.mongoose.events;

import java.sql.Date;
import java.util.List;

import com.autowares.events.Attachment;
import com.autowares.events.AwiEvent;
import com.autowares.events.EventType;
import com.autowares.events.Notification;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseViperEvent implements AwiEvent {
	
	private Double wdInvoiceCostExtended;
	private String zone;
	private String buildingCode;
	private Long orderID;
	private Long location;
	private String progSource;
	private Double core;
	private String upc;
	private Double wdCore;
	private String customerNumber;
	private String shelf;
	private Long moaPartHeaderId;
	private String action;
	private Double wdTrueCost;
	private String truckRun;
	private String labelNumber;
	private String jobFlag;
	private Date timeStamp;
	private String ecommerceAccount;
	private Integer secondsElapsed;
	private String employeeName;
	private String progName;
	private String employeeNumber;
	private Double wdInvoiceCost;
	private Double wdCostExtended;
	private String pullerOption;
	private Double wdCost;
	private Integer quantity;
	private Double billPrice;
	private String warehouseName;
	private Double priceExtended;
	private String toteID;
	private String partNumber;
	private Double wdTrueCostExtended;
	private String vendor;
	private String partDescription;
	private String xmlOrderID;
	private String putAwayFile;
	private String putawayType;
	private String rackLoc;
	private String packSlipNumber;
	private String purchaseOrderNumber;
	private String originalPurchaseOrderNumber;
	private EventType eventType;

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

	public String getBuildingCode() {
		return buildingCode;
	}

	public void setBuildingCode(String buildingCode) {
		this.buildingCode = buildingCode;
	}

	public Long getOrderID() {
		return orderID;
	}

	public void setOrderID(Long orderID) {
		this.orderID = orderID;
	}

	public Long getLocation() {
		return location;
	}

	public void setLocation(Long location) {
		this.location = location;
	}

	public String getProgSource() {
		return progSource;
	}

	public void setProgSource(String progSource) {
		this.progSource = progSource;
	}

	public Double getCore() {
		return core;
	}

	public void setCore(Double core) {
		this.core = core;
	}

	public String getUpc() {
		return upc;
	}

	public void setUpc(String upc) {
		this.upc = upc;
	}

	public Double getWdCore() {
		return wdCore;
	}

	public void setWdCore(Double wdCore) {
		this.wdCore = wdCore;
	}

	public String getCustomerNumber() {
		return customerNumber;
	}

	public void setCustomerNumber(String customerNumber) {
		this.customerNumber = customerNumber;
	}

	public String getShelf() {
		return shelf;
	}

	public void setShelf(String shelf) {
		this.shelf = shelf;
	}

	public Long getMoaPartHeaderId() {
		return moaPartHeaderId;
	}

	public void setMoaPartHeaderId(Long moaPartHeaderId) {
		this.moaPartHeaderId = moaPartHeaderId;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public Double getWdTrueCost() {
		return wdTrueCost;
	}

	public void setWdTrueCost(Double wdTrueCost) {
		this.wdTrueCost = wdTrueCost;
	}

	public String getTruckRun() {
		return truckRun;
	}

	public void setTruckRun(String truckRun) {
		this.truckRun = truckRun;
	}

	public String getLabelNumber() {
		return labelNumber;
	}

	public void setLabelNumber(String labelNumber) {
		this.labelNumber = labelNumber;
	}

	public String getJobFlag() {
		return jobFlag;
	}

	public void setJobFlag(String jobFlag) {
		this.jobFlag = jobFlag;
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getEcommerceAccount() {
		return ecommerceAccount;
	}

	public void setEcommerceAccount(String ecommerceAccount) {
		this.ecommerceAccount = ecommerceAccount;
	}

	public Integer getSecondsElapsed() {
		return secondsElapsed;
	}

	public void setSecondsElapsed(Integer secondsElapsed) {
		this.secondsElapsed = secondsElapsed;
	}

	public String getEmployeeName() {
		return employeeName;
	}

	public void setEmployeeName(String employeeName) {
		this.employeeName = employeeName;
	}

	public String getProgName() {
		return progName;
	}

	public void setProgName(String progName) {
		this.progName = progName;
	}

	public String getEmployeeNumber() {
		return employeeNumber;
	}

	public void setEmployeeNumber(String employeeNumber) {
		this.employeeNumber = employeeNumber;
	}

	public Double getWdInvoiceCost() {
		return wdInvoiceCost;
	}

	public void setWdInvoiceCost(Double wdInvoiceCost) {
		this.wdInvoiceCost = wdInvoiceCost;
	}

	public Double getWdCostExtended() {
		return wdCostExtended;
	}

	public void setWdCostExtended(Double wdCostExtended) {
		this.wdCostExtended = wdCostExtended;
	}

	public String getPullerOption() {
		return pullerOption;
	}

	public void setPullerOption(String pullerOption) {
		this.pullerOption = pullerOption;
	}

	public Double getWdCost() {
		return wdCost;
	}

	public void setWdCost(Double wdCost) {
		this.wdCost = wdCost;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public Double getBillPrice() {
		return billPrice;
	}

	public void setBillPrice(Double billPrice) {
		this.billPrice = billPrice;
	}

	public String getWarehouseName() {
		return warehouseName;
	}

	public void setWarehouseName(String warehouseName) {
		this.warehouseName = warehouseName;
	}

	public Double getPriceExtended() {
		return priceExtended;
	}

	public void setPriceExtended(Double priceExtended) {
		this.priceExtended = priceExtended;
	}

	public String getToteID() {
		return toteID;
	}

	public void setToteID(String toteID) {
		this.toteID = toteID;
	}

	public String getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}

	public Double getWdTrueCostExtended() {
		return wdTrueCostExtended;
	}

	public void setWdTrueCostExtended(Double wdTrueCostExtended) {
		this.wdTrueCostExtended = wdTrueCostExtended;
	}

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public String getPartDescription() {
		return partDescription;
	}

	public void setPartDescription(String partDescription) {
		this.partDescription = partDescription;
	}

	public String getXmlOrderID() {
		return xmlOrderID;
	}

	public void setXmlOrderID(String xmlOrderID) {
		this.xmlOrderID = xmlOrderID;
	}

	public String getPutAwayFile() {
		return putAwayFile;
	}

	public void setPutAwayFile(String putAwayFile) {
		this.putAwayFile = putAwayFile;
	}

	public String getPutawayType() {
		return putawayType;
	}

	public void setPutawayType(String putawayType) {
		this.putawayType = putawayType;
	}

	public String getRackLoc() {
		return rackLoc;
	}

	public void setRackLoc(String rackLoc) {
		this.rackLoc = rackLoc;
	}

	public String getPackSlipNumber() {
		return packSlipNumber;
	}

	public void setPackSlipNumber(String packSlipNumber) {
		this.packSlipNumber = packSlipNumber;
	}

	public String getPurchaseOrderNumber() {
		return purchaseOrderNumber;
	}

	public void setPurchaseOrderNumber(String purchaseOrderNumber) {
		this.purchaseOrderNumber = purchaseOrderNumber;
	}

	public String getOriginalPurchaseOrderNumber() {
		return originalPurchaseOrderNumber;
	}

	public void setOriginalPurchaseOrderNumber(String originalPurchaseOrderNumber) {
		this.originalPurchaseOrderNumber = originalPurchaseOrderNumber;
	}

	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}

	@Override
	public String getEventKey() {
		return null;
	}

	@Override
	public EventType getEventType() {
		return eventType;
	}

	@Override
	public java.util.Date getEventDate() {
		return null;
	}

	@Override
	public String getMessage() {
		return null;
	}

	@Override
	public List<Notification> getNotifications() {
		return null;
	}

	@Override
	public List<Attachment> getAttachments() {
		return null;
	}

}
