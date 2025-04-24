package com.autowares.mongoose.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.InventoryStateManager;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.productinventory.client.ProductInventoryClient;
import com.autowares.productinventory.model.Allocation;
import com.autowares.productinventory.model.Inventory;
import com.autowares.servicescommon.client.DiscoverService;

@Component
@DiscoverService(name = "productinventory", path = "/productinventory/inventory")
public class ProductInventoryService extends ProductInventoryClient implements InventoryStateManager {
	
	public ProductInventoryService() {
//		this.withLocalService();
//		this.withPort(9999);
	}

	@Override
	public Page<Inventory> findInventory(Long productId) {
		return this.find(null, null, productId, null, null);
	}

	@Override
	public Inventory persist(Inventory inventory) {
		return this.update(inventory);
	}

	@Override
	public Page<Allocation> findAllocations(String transactionId, Long productId) {
		return this.find(transactionId, productId);
	}

	@Override
	public void unallocate(LineItemContext lineItemContext) {
		String transactionId = lineItemContext.getContext().getDocumentId();
    	Long productId = lineItemContext.getProductId();
    	for (Allocation allocation: findAllocations(transactionId, productId)) {
    		unallocate(allocation.getId());
    	}		
	}

	@Override
	public Allocation allocate(Availability availability) {
		String locationId = availability.getFulfillmentLocation().getShipment().getTransportSource().getLocationId();
		Long productId = availability.getLineItem().getProductId();
		//TODO this only works if being executed on an order context, the transactionId could be the UUID generated id in the transactionContext
		// or better yet the transactionContext's id should be the same generated ID as the purchase order document
		String transactionId = availability.getLineItem().getContext().getDocumentId();
		Allocation allocation = new Allocation();
		allocation.setQuantity(availability.getFillQuantity());
		allocation.setTransactionId(transactionId);
		this.allocate(allocation, locationId, productId);
		return allocation;
	}

}
