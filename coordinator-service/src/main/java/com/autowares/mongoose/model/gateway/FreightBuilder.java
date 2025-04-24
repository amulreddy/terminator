package com.autowares.mongoose.model.gateway;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.autowares.servicescommon.model.Freight;
import com.autowares.servicescommon.model.LocationType;
import com.autowares.servicescommon.model.RunType;
import com.autowares.servicescommon.model.SystemType;

public class FreightBuilder {
	
	private Freight location = new Freight();

	public FreightBuilder withLocation(Freight location) {
		this.location = location;
		return this;
	}
	
	public FreightBuilder withLocationName(String locationName) {
		this.location.setLocationName(locationName);
		return this;
	}
	
	public FreightBuilder withEstimatedDeliveryDate(ZonedDateTime estimatedDate) {
		this.location.setEstimatedDeliveryDateTime(estimatedDate);
		return this;
	}
	
	public FreightBuilder withEstimatedShipDateTime(ZonedDateTime shipDateTime) {
		this.location.setEstimatedShipDateTime(shipDateTime);
		return this;
	}
	
	public FreightBuilder withExpireTime(ZonedDateTime expireTime) {
		this.location.setExpireTime(expireTime);
		return this;
	}
	
	public FreightBuilder withAvailableQuantity(Integer availableQuantity) {
		this.location.setAvailableQuantity(availableQuantity);
		return this;
	}
	
	public FreightBuilder withPlannedFillQuantity(Integer fillQuantity) {
		this.location.setPlannedFillQuantity(fillQuantity);
		return this;
	}
	
	public FreightBuilder withShipQuantity(Integer fillQuantity) {
		this.location.setShipQuantity(fillQuantity);
		return this;
	}
	
	public FreightBuilder withWarehouseNumber(Integer warehouseNumber) {
		this.location.setWarehouseNumber(warehouseNumber);
		return this;
	}
	
	public FreightBuilder withRunType(RunType runType) {
		this.location.setDeliveryRunType(runType);
		return this;
	}
	
	public FreightBuilder withSequence(Integer sequence) {
		this.location.setSequence(sequence);
		return this;
	}
	
	public FreightBuilder withLocationType(LocationType locationType) {
		this.location.setLocationType(locationType);
		return this;
	}
	
	public FreightBuilder withSystemType(SystemType systemType) {
		this.location.setSystemType(systemType);
		return this;
	}
	
	public FreightBuilder withPrice(BigDecimal price) {
		this.location.setPrice(price);
		return this;
	}
	
	public FreightBuilder withProcurementGroupId(UUID id) {
		this.location.setProcurementGroupId(id);
		return this;
	}
	
	public FreightBuilder withDescription(String description) {
		this.location.setDescription(description);
		return this;
	}
	
	public FreightBuilder withDocumentReferenceIds(List<String> documentReferenceId) {
		this.location.setDocumentReferenceIds(documentReferenceId);
		return this;
	}
	
	public FreightBuilder withDeliveryRunName(String deliveryRunName) {
		this.location.setDeliveryRunName(deliveryRunName);
		return this;
	}
	
	public FreightBuilder withDocumentReferenceId(String documentReferenceId) {
		this.location.setDocumentReferenceId(documentReferenceId);
		return this;
	}

	public Freight build() {
		return location;
	}

}
