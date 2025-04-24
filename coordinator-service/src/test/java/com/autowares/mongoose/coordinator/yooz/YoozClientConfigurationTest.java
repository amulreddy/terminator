package com.autowares.mongoose.coordinator.yooz;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.autowares.mongoose.config.YoozConfig;

@SpringBootTest
public class YoozClientConfigurationTest {
	
	@Autowired
	YoozConfig yoozConfig;
	
	@Test
	public void testConfiguration() {
		
		assertNotNull(yoozConfig);
		assertEquals("https://us1.getyooz.com", yoozConfig.getApiurl());
		
	}

}
