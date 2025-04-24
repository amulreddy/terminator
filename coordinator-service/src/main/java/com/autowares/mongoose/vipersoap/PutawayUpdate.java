package com.autowares.mongoose.vipersoap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "PutawayUpdate", namespace = "urn:kcml-Mongoose")
@XmlAccessorType (XmlAccessType.NONE)
public class PutawayUpdate {

	@XmlAttribute(name = "encodingStyle", namespace = "http://schemas.xmlsoap.org/soap/envelope/")
	private String encodingStyle = "http://schemas.xmlsoap.org/soap/encoding/";
	@XmlElement(name = "PutawayFile")
	private String putawayFile;
	@XmlElement(name = "VendorSubcode")
	private String vendorSubcode;
	@XmlElement(name = "PartNumber")
	private String partNumber;
	@XmlElement(name = "EmployeeId")
	private String employeeId;
	@XmlElement(name = "ReceivedQuantity")
	private String receivedQuantity;
	@XmlElement(name = "skipPartUpdate")
	private String skipPartUpdate;
	@XmlElement(name = "AddPart")
	private String addPart = "1";

	public String getEncodingStyle() {
		return encodingStyle;
	}

	public void setEncodingStyle(String encodingStyle) {
		this.encodingStyle = encodingStyle;
	}

	public String getPutawayFile() {
		return putawayFile;
	}

	public void setPutawayFile(String putawayFile) {
		this.putawayFile = putawayFile;
	}
	public String getVendorSubcode() {
		return vendorSubcode;
	}

	public void setVendorSubcode(String vendorSubcode) {
		this.vendorSubcode = vendorSubcode;
	}

	public String getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}

	public String getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(String employeeId) {
		this.employeeId = employeeId;
	}

	public String getReceivedQuantity() {
		return receivedQuantity;
	}

	public void setReceivedQuantity(String receivedQuantity) {
		this.receivedQuantity = receivedQuantity;
	}
	
	public String getSkipPartUpdate() {
		return skipPartUpdate;
	}

	public void setSkipPartUpdate(String skipPartUpdate) {
		this.skipPartUpdate = skipPartUpdate;
	}

	public String getAddPart() {
		return addPart;
	}

	public void setAddPart(String addPart) {
		this.addPart = addPart;
	}

}
