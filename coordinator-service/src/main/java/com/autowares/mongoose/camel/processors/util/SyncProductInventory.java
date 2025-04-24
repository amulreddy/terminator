package com.autowares.mongoose.camel.processors.util;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.apis.partservice.PartAvailability;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.service.ProductInventoryService;
import com.autowares.productinventory.model.Inventory;

@Component
public class SyncProductInventory implements Processor {

	@Autowired
	ProductInventoryService inventoryStateManager;

	private static Logger log = LoggerFactory.getLogger(SyncProductInventory.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		try {
			Availability availability = exchange.getIn().getBody(Availability.class);
			LineItemContext lineItem = availability.getLineItem();
			String locationId = null;
			String internalLocation = null;

			if (availability.getFulfillmentLocation().getShipment() != null) {
				locationId = availability.getFulfillmentLocation().getShipment().getTransportSource().getLocationId();
			}
			if (locationId != null) {
				if (availability.getPartAvailability() != null) {
					PartAvailability partAvailability = availability.getPartAvailability();
					internalLocation = partAvailability.getZone() + ":" + partAvailability.getAisle() + ":"
							+ partAvailability.getRack() + ":" + partAvailability.getLocation();
				}
				Inventory inventory = new Inventory(locationId, internalLocation, lineItem.getProductId());
				inventory.setQuantityOnShelf(availability.getQuantityOnHand());
				inventory = inventoryStateManager.persist(inventory);
				if (availability.getFillQuantity() > 0) {
					inventoryStateManager.allocate(availability);
				}
			} else {
				log.error("No locationId for: " + availability.getFulfillmentLocation().getLocation());
			}
		} catch (Exception e) {
		}
	}

}
