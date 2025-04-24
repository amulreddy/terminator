package com.autowares.mongoose.coordinator.routes;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpointsAndSkip;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest
@CamelSpringBootTest
@MockEndpointsAndSkip("file:output")
@ActiveProfiles("unitTest")
/*
 * Move to Integration testing if keeping
 */
@Disabled
public class BasicRouteTest {
	

    @Autowired
    private ProducerTemplate template;

    @EndpointInject("mock:file:output")
    private MockEndpoint mock;
    
    @Test
    public void whenSendBody_thenGreetingReceivedSuccessfully() throws InterruptedException {
        mock.expectedBodiesReceived("Hello Java Buddies!");
        template.sendBody("direct:basicRouteTest", null);
        mock.assertIsSatisfied();
    }
	
}
