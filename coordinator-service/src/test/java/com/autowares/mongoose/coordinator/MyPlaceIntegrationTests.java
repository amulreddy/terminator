package com.autowares.mongoose.coordinator;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.autowares.myplaceintegration.client.MyPlaceDDIClient;
import com.autowares.myplaceintegration.model.DdsResponse;
import com.autowares.myplaceintegration.model.MyPlaceOrderNotification;
import com.autowares.servicescommon.util.PrettyPrint;

/*
 * Move to Integration testing if keeping
 */
@Disabled
public class MyPlaceIntegrationTests {
	
	private MyPlaceDDIClient client = new MyPlaceDDIClient();
	
	@Test
	public void testOrderNotification() {
		MyPlaceOrderNotification notification = new MyPlaceOrderNotification();
		notification.setDocumentId("testDocumentId");
		DdsResponse response = client.updateMyPlace(notification);
		PrettyPrint.print(response);
	}

}
