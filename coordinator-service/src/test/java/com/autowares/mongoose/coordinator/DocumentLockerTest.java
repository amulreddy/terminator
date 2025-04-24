package com.autowares.mongoose.coordinator;

import static org.junit.Assert.assertTrue;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.autowares.ServiceDiscovery;
import com.autowares.mongoose.service.LockedDocuments;

public class DocumentLockerTest extends CamelTestSupport {
	
	private LockedDocuments lockedDocuments;
	
	@BeforeEach
	public void setUp() throws Exception {
		ServiceDiscovery.setDomain("testconsul");
		lockedDocuments = new LockedDocuments();
		super.setUp();
	}
	
	@Test
	public void testRoute() throws Exception {
		assertTrue(context().isStarted());
		MockEndpoint mockResult = getMockEndpoint("mock:result");
		mockResult.expectedMessageCount(1);
		template.sendBodyAndHeader("direct:start","Test Message","messageId","UniqueId2");
		assertMockEndpointsSatisfied();
	}
	
	@Override
	protected CamelContext createCamelContext() throws Exception {
		
		return super.createCamelContext();
	}
	
	@Override
	protected RoutesBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {

			@Override
			public void configure() throws Exception {
				from("direct:start")
				  .idempotentConsumer(header("messageId"),lockedDocuments)
				  .log("testing")
				  .to("mock:result");
			}}; 
	}

}
