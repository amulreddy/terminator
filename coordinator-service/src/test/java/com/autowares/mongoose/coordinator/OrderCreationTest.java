package com.autowares.mongoose.coordinator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.autowares.servicescommon.model.DeliveryMethod;
import com.autowares.servicescommon.model.RunType;
import com.autowares.servicescommon.model.ServiceClass;

public class OrderCreationTest {
	
	@Test
	public void runTypeTest() {
		
		assertEquals(RunType.find("P"), RunType.find(DeliveryMethod.CustomerPickUp, ServiceClass.Standard));
		
	}

}
