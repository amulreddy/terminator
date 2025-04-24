package com.autowares.mongoose.command;

import java.net.URL;
import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

import com.autowares.ServiceDiscovery;
import com.autowares.servicescommon.client.BaseResillience4JClient;

public class PurchaseRestrictionsClient extends BaseResillience4JClient {

	public static class PurchaseRestrictionResponse {

		private boolean restricted = false;
		private String rejectionReason;

		public boolean isRestricted() {
			return restricted;
		}

		public void setRestricted(boolean status) {
			this.restricted = status;
		}

		public String getRejectionReason() {
			return rejectionReason;
		}

		public void setRejectionReason(String rejectionReason) {
			this.rejectionReason = rejectionReason;
		}

	}

	public PurchaseRestrictionResponse purchaseRestriction(String awAccountNo, String vendorCode, String partNumber) {
		Supplier<PurchaseRestrictionResponse> supplier = () -> this.getPurchaseRestrictionProtected(awAccountNo,
				vendorCode, partNumber);
		return decorate(supplier, Duration.ofSeconds(5));
	}

	private PurchaseRestrictionResponse getPurchaseRestrictionProtected(String awAccountNo, String vendorCode,
			String partNumber) {
		RestTemplate template = new RestTemplate();
		UriBuilder uriBuilder = initializeBuilder();
		uriBuilder.queryParam("awAccountNo", awAccountNo);
		uriBuilder.queryParam("vendorSubCode", vendorCode);
		uriBuilder.queryParam("partNumber", partNumber);
		try {
			ResponseEntity<PurchaseRestrictionResponse> response = template.getForEntity(uriBuilder.build(),
					PurchaseRestrictionResponse.class);
			return response.getBody();
		} catch (Exception e) {
			HttpClientErrorException clientError = getHttpClientError(e);
			if (clientError != null && clientError.getStatusCode() == HttpStatus.NOT_FOUND) {
				return null;
			}
			throw e;
		}

	}

	private UriBuilder initializeBuilder() {
		URL base = ServiceDiscovery.resolveServiceURL("purchase-restrictions");
		UriBuilder uriBuilder = new DefaultUriBuilderFactory().uriString(String.valueOf(base));
		uriBuilder.path("/purchase-restrictions/checkstore");
		return uriBuilder;
	}

}
