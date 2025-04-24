package com.autowares.mongoose.service;

import org.springframework.stereotype.Component;

import com.autowares.logistix.commands.ShipmentClient;
import com.autowares.servicescommon.client.DiscoverService;

@Component
@DiscoverService(name = "logistix", path = "/logistix/shipment")
public class LogistixService extends ShipmentClient {
	
	public LogistixService() {
//		this.withLocalService();
//		this.withPort(9999);
	}

}
