package com.autowares.mongoose.coordinator;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.autowares.mongoose.client.DummyClient;

@Disabled
public class ClientErrorTests {
	
	private DummyClient client = new DummyClient();
	
	@Test
	public void testTimeout() {
		client.timeOut();
	}
	
	@Test
	public void test500() {
		client.fiveHundy();
	}
	
	@Test
	public void testConnectFailure() {
		client.noConnection();
	}

}
