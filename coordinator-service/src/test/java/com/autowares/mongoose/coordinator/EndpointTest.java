package com.autowares.mongoose.coordinator;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import com.autowares.ServiceDiscovery;
import com.autowares.servicescommon.util.PrettyPrint;

public class EndpointTest {
	
	@Test
	public void testUri() throws URISyntaxException {
		String service = "awi://ids/ids/rest";
		URI endpoint = new URI(service);
		System.out.println(endpoint);
		System.out.println(endpoint.getHost());
		System.out.println(endpoint.getPath());
	}
	
	@Test
	public void serviceDiscovery() {
		ServiceDiscovery.setDomain("testconsul");
		PrettyPrint.print(ServiceDiscovery.resolveServiceURL("wms-orders"));
	}

}
