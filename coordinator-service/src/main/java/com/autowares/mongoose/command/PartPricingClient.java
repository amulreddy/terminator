package com.autowares.mongoose.command;

import java.net.URL;
import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

import com.autowares.ServiceDiscovery;
import com.autowares.mongoose.model.PartPricingResponse;
import com.autowares.servicescommon.client.BaseResillience4JClient;

public class PartPricingClient extends BaseResillience4JClient {
	public PartPricingResponse pricePart(String awAccountNumber, Long partHeaderId) {
		Supplier<PartPricingResponse> supplier = () -> this.callPartPrice(awAccountNumber, partHeaderId);
		return decorate(supplier, Duration.ofSeconds(5));
	}

	private PartPricingResponse callPartPrice(String awAccountNumber, Long partHeaderId) {
		RestTemplate template = new RestTemplate();
		UriBuilder uriBuilder = initializeBuilder();
		uriBuilder.pathSegment("part");
		uriBuilder.queryParam("awAccountNumber", awAccountNumber);
		uriBuilder.queryParam("partHeaderId", partHeaderId);
		ResponseEntity<PartPricingResponse> response = template.getForEntity(uriBuilder.build(),
				PartPricingResponse.class);
		return response.getBody();
	}

	private UriBuilder initializeBuilder() {
		URL base = ServiceDiscovery.resolveServiceURL("part-pricing");
		UriBuilder uriBuilder = new DefaultUriBuilderFactory().uriString(String.valueOf(base));
		uriBuilder.path("/part-pricing");
		return uriBuilder;
	}
}
