 package com.autowares.mongoose.coordinator.yooz;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.autowares.mongoose.command.YoozClient;
import com.autowares.mongoose.model.yooz.YoozDocument;

public class Yooz2Test {

	@Test
	public void testYoozConnectionTest() throws Exception {
		RestTemplate rt = new RestTemplate();
		
		UriBuilder uriBuilder = 
				UriBuilder.fromUri("https://us1.getyooz.com/auth/realms/yooz/protocol/openid-connect/token");

		LinkedMultiValueMap<String, String> data = new LinkedMultiValueMap<String, String>();

		data.add("grant_type", "password");
		data.add("client_id", "yooz-public-api");
		data.add("username", "Autowares.IT@noemail.dsbl");
		data.add("password", "RisingYoozAutowares2024!");

		MultiValueMap<String, String> headers = new HttpHeaders();

		headers.add("ContentType", MediaType.APPLICATION_FORM_URLENCODED_VALUE);

		HttpEntity<LinkedMultiValueMap<String, String>> entity = new HttpEntity<>(data, headers);

		Map response = rt.postForEntity(uriBuilder.build(), entity, Map.class).getBody();

		System.out.println(response);

		String auth = "Bearer " + response.get("access_token");
		System.out.println(auth);
		headers = new HttpHeaders();
		headers.add("Authorization", auth);
		headers.add("ApplicationId", "e829308f-5638-4fc9-9d51-1a2a30c73f93");
		uriBuilder = UriBuilder.fromUri("https://us1.getyooz.com/yooz/v2/api/documentTypes");

		ParameterizedTypeReference<String> type = new ParameterizedTypeReference<String>() {
		};

		HttpEntity<String> callData = new HttpEntity<>(headers);

		// Execute the request
		ResponseEntity<String> r = rt.exchange(uriBuilder.build().toASCIIString(), HttpMethod.GET, callData, type);

		// Print response
		System.out.println(r.getBody());
	}
	
	@Test
	public void clientTest() {
		YoozClient yoozClient = new YoozClient();
		List response = yoozClient.getDocumentTypes();
		for (Object member:response) {
			System.out.println(member);
		}
	}

}
