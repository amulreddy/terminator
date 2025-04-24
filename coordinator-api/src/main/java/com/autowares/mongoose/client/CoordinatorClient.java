package com.autowares.mongoose.client;

import java.time.Duration;

import org.springframework.web.util.UriBuilder;

import com.autowares.servicescommon.client.BaseResillience4JClient;
import com.autowares.servicescommon.client.DiscoverService;
import com.autowares.xmlgateway.model.InquiryRequest;
import com.autowares.xmlgateway.model.InquiryResponse;
import com.autowares.xmlgateway.model.OrderRequest;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

@DiscoverService(name = "coordinator", path = "/coordinator")
public class CoordinatorClient extends BaseResillience4JClient {

	public CoordinatorClient() {
//		this.withLocalService();
//		this.withPort(8282);
		this.circuitBreaker = CircuitBreaker.ofDefaults("Circuit Breaker!");
		this.bulkhead = ThreadPoolBulkhead.of("bulkhead", ThreadPoolBulkheadConfig.custom().coreThreadPoolSize(10)
				.maxThreadPoolSize(10).queueCapacity(1).build());
	}

	public String processDocument(String documentId) {
		UriBuilder uriBuilder = getUriBuilder();

		uriBuilder.pathSegment("processDocument");
		uriBuilder.queryParam("documentId", documentId);
		String response = getForObject(uriBuilder, String.class, Duration.ofSeconds(60));
		return response;
	}
	
	public InquiryResponse quote(InquiryRequest quoteRequest) {
		UriBuilder uriBuilder = getUriBuilder();
		uriBuilder.pathSegment("quote");
		InquiryResponse response = postForObject(uriBuilder, quoteRequest, InquiryResponse.class);
		return response;
	}
	
	public Object processOrder(OrderRequest quoteRequest) {
		UriBuilder uriBuilder = getUriBuilder();
		uriBuilder.pathSegment("processOrder");
		Object response = postForObject(uriBuilder, quoteRequest, Object.class);
		return response;
	}
	

	public InquiryResponse inquire(InquiryRequest inquiryRequest) {
		UriBuilder uriBuilder = getUriBuilder();
		uriBuilder.pathSegment("inquire");
		InquiryResponse response = postForObject(uriBuilder, inquiryRequest, InquiryResponse.class);
		return response;
	}

}
