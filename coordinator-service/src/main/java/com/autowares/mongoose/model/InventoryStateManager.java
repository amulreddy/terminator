package com.autowares.mongoose.model;

import org.springframework.data.domain.Page;

import com.autowares.productinventory.model.Allocation;
import com.autowares.productinventory.model.Inventory;

public interface InventoryStateManager {

	Page<Inventory> findInventory(Long productId);
	Inventory persist(Inventory inventory);
	Page<Allocation> findAllocations(String transactionId, Long productId);
	void unallocate(LineItemContext lineItemContext);
	Allocation allocate(Availability availability);

}
