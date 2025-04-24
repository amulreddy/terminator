package com.autowares.mongoose.client;

import java.time.Duration;

import org.springframework.web.util.UriBuilder;

import com.autowares.servicescommon.api.ApiMessage;
import com.autowares.servicescommon.client.BaseResillience4JClient;
import com.autowares.servicescommon.client.DiscoverService;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

@DiscoverService(name = "coordinator", path = "/mongoose/dummy")
public class DummyClient extends BaseResillience4JClient {
	
	
	public DummyClient() {
		this.withLocalService();
		this.withPort(8282);
		this.circuitBreaker = CircuitBreaker.ofDefaults("Greg's circuit breaker!");
		this.bulkhead = ThreadPoolBulkhead.of("bulkhead",
				ThreadPoolBulkheadConfig.custom().coreThreadPoolSize(10).maxThreadPoolSize(10).queueCapacity(1).build());
	}
	
	public ApiMessage timeOut() {
		UriBuilder uriBuilder = getUriBuilder();
		uriBuilder.pathSegment("timeout");
		return this.getForObject(uriBuilder, ApiMessage.class, Duration.ofMillis(100));
	}
	
	public String fiveHundy() {
		UriBuilder uriBuilder = getUriBuilder();
		uriBuilder.pathSegment("fiveHundy");
		return this.getForObject(uriBuilder, String.class);
	}
	
	public String noConnection() {
		UriBuilder uriBuilder = getUriBuilder();
		uriBuilder.port(100);
		return this.getForObject(uriBuilder, String.class);
	}

}
