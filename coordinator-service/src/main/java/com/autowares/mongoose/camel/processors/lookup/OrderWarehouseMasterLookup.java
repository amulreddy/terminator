package com.autowares.mongoose.camel.processors.lookup;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.apis.ids.command.WarehouseMasterClient;
import com.autowares.apis.ids.model.WarehouseMaster;
import com.autowares.mongoose.model.FulfillmentLocationContext;

@Component
public class OrderWarehouseMasterLookup implements Processor {

	WarehouseMasterClient warehouseMasterClient = new WarehouseMasterClient();
	private static List<WarehouseMaster> warehouseMasters;

	@Override
	public void process(Exchange exchange) throws Exception {
		FulfillmentLocationContext fillContext = exchange.getIn().getBody(FulfillmentLocationContext.class);
		fillContext.setWarehouseMaster(getWarehouseMaster(fillContext));
	}

	public WarehouseMaster getWarehouseMaster(FulfillmentLocationContext fillContext) {
		for (WarehouseMaster wm : getWarehouseMasters()) {
			if (fillContext.getLocation() != null && fillContext.getLocation().equals(wm.getBuildingMnemonic())) {
				// TODO This doesnt map cleanly for GRR
				if ("GRR".equals(fillContext.getLocation())) {
					if (wm.getWarehouseNumber() == 1) {
						return wm;
					}
				} else {
					return wm;
				}
			}
		}
		return null;
	}

	public List<WarehouseMaster> getWarehouseMasters() {
		if (warehouseMasters == null || warehouseMasters.isEmpty()) {
			warehouseMasters = warehouseMasterClient.findWarehouse(null, null, null, true).stream()
					.sorted(Comparator.comparingInt(WarehouseMaster::getWarehouseNumber)).collect(Collectors.toList());
		}
		return warehouseMasters;
	}

}
