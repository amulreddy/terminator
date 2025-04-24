package com.autowares.mongoose.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.autowares.apis.partservice.viperpartupdate.PartUpdate;
import com.autowares.mongoose.command.ViperPartUpdateClient;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.InventoryStateManager;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.productinventory.model.Allocation;
import com.autowares.productinventory.model.Inventory;
import com.autowares.servicescommon.model.LocationType;

@Component
public class ViperInventoryStateManager implements InventoryStateManager {

	ViperPartUpdateClient viperPartUpdateClient = new ViperPartUpdateClient();

	@Override
	public Page<Inventory> findInventory(Long productId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Inventory persist(Inventory inventory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<Allocation> findAllocations(String transactionId, Long productId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void unallocate(LineItemContext lineItemContext) {
		Availability availability = lineItemContext.getAvailability().get(0);
		String warehouseNumber = String
				.valueOf(availability.getMoaOrderDetail().getWarehouse());
		PartUpdate partUpdate = new PartUpdate();
		partUpdate.setPartNumber(lineItemContext.getPartNumber());
		partUpdate.setVendorCode(lineItemContext.getVendorCodeSubCode());
		partUpdate.setWarehouseNumber(warehouseNumber);
		partUpdate.setColumnName1("Allocate");
		partUpdate.setColumnValue1(String.valueOf(availability.getFillQuantity() * -1));
		viperPartUpdateClient.updatePart(partUpdate);
	}

	@Override
	public Allocation allocate(Availability availability) {
		if (LocationType.Warehouse.equals(availability.getFulfillmentLocation().getLocationType())) {
			Integer warehouseNumber = (int) availability.getPartAvailability().getWarehouseNumber();
			PartUpdate partUpdate = new PartUpdate();
			partUpdate.setVendorCode(availability.getLineItem().getVendorCodeSubCode());
			partUpdate.setPartNumber(availability.getLineItem().getPartNumber());
			partUpdate.setWarehouseNumber(String.valueOf(warehouseNumber));
			partUpdate.setColumnName1("Allocate");
			partUpdate.setColumnValue1(String.valueOf(availability.getFillQuantity()));
			String response = null;
			response = viperPartUpdateClient.updatePart(partUpdate);
			if ("OK".equals(response)) {
				Allocation allocation = new Allocation();
				allocation.setQuantity(availability.getFillQuantity());
				return allocation;
			}
		}

		return null;
	}

}
