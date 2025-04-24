package com.autowares.mongoose.command;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

import com.autowares.mongoose.config.YoozConfig;
import com.autowares.mongoose.model.yooz.DataContainer;
import com.autowares.servicescommon.client.BaseResillience4JClient;
import com.autowares.servicescommon.util.PrettyPrint;

public class YoozClient extends BaseResillience4JClient {

	private String yoozAuthenticationToken = null;
	private String userName = "Autowares.IT@noemail.dsbl";
	private String password = "RisingYoozAutowares2024!";
	private String clientId = "yooz-public-api";
	private String applicationId = "e829308f-5638-4fc9-9d51-1a2a30c73f93";
	private String apiurl = "https://us1.getyooz.com";
	private ZonedDateTime tokenRefreshTime = null;

	public void withYoozConfig(YoozConfig yoozConfig) {
		apiurl = yoozConfig.getApiurl();
		applicationId = yoozConfig.getApplicationId();
		clientId = yoozConfig.getClientId();
		password = yoozConfig.getPassword();
		userName = yoozConfig.getUserName();
	}
	
	@SuppressWarnings("unchecked")
	public List<String> getDocumentTypes() {

		UriBuilder uriBuilder = getUriBuilder();
		uriBuilder.pathSegment("yooz", "v2", "api", "documentTypes");

		return getForObject(uriBuilder, List.class, Duration.ofSeconds(30l));

	}

	@SuppressWarnings("rawtypes")
	public Map importFile(Object data, String fileName) {
		UriBuilder uriBuilder = getUriBuilder();

		String orgUnit = "AWI";

		uriBuilder.pathSegment("yooz", "v2", "api", "orgUnits", orgUnit, "importPurchaseOrders");
		this.authenticate();

		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", this.yoozAuthenticationToken);
		headers.add("applicationId", this.applicationId);
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

		ByteArrayResource fileAsResource = new ByteArrayResource(data.toString().getBytes()) {
			@Override
			public String getFilename() {
				return fileName;
			}
		};

		body.add("file", fileAsResource);

		HttpEntity<?> callData = new HttpEntity<>(body, headers);
		return decorate(() -> restTemplate.postForEntity(uriBuilder.build(), callData, Map.class),
				Duration.ofSeconds(30l)).getBody();
	}
	
	@SuppressWarnings("rawtypes")
	public Map importDataContainer(DataContainer data) {
		UriBuilder uriBuilder = getUriBuilder();

		uriBuilder.pathSegment("yooz", "v2", "api", "documents");
		this.authenticate();

		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", this.yoozAuthenticationToken);
		headers.add("applicationId", this.applicationId);
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

		ByteArrayResource fileAsResource = new ByteArrayResource(PrettyPrint.toString(data).getBytes()) {
			@Override
			public String getFilename() {
				return "document.json";
			}
		};

		body.add("documentData", fileAsResource);

		HttpEntity<?> callData = new HttpEntity<>(body, headers);
		return decorate(() -> restTemplate.postForEntity(uriBuilder.build(), callData, Map.class),
				Duration.ofSeconds(30l)).getBody();
	}

	public void authenticate() {
		if (this.yoozAuthenticationToken != null) {
			if (tokenRefreshTime != null) {
				if (tokenRefreshTime.isAfter(ZonedDateTime.now().minusSeconds(90l))) {
					// If the token has been refreshed in the last 90 seconds we do not need to
					// re-authenticate.
					return;
				}
			}
		}
		UriBuilder uriBuilder = getUriBuilder();
		uriBuilder.pathSegment("auth", "realms", "yooz", "protocol", "openid-connect", "token");

		LinkedMultiValueMap<String, String> authenticationRequest = new LinkedMultiValueMap<String, String>();
		authenticationRequest.add("grant_type", "password");
		authenticationRequest.add("client_id", clientId);
		authenticationRequest.add("username", userName);
		authenticationRequest.add("password", password);

		MultiValueMap<String, String> headers = new HttpHeaders();
		headers.add("ContentType", MediaType.APPLICATION_FORM_URLENCODED_VALUE);

		HttpEntity<LinkedMultiValueMap<String, String>> entity = new HttpEntity<>(authenticationRequest, headers);

		@SuppressWarnings("rawtypes")
		Map response = decorate(() -> restTemplate.postForEntity(uriBuilder.build(), entity, Map.class).getBody(),
				Duration.ofSeconds(30l));

		yoozAuthenticationToken = "Bearer " + response.get("access_token");

		setTokenRefreshTime(ZonedDateTime.now());

	}

	@Override
	public UriBuilder getUriBuilder() {
		UriBuilder uriBuilder = new DefaultUriBuilderFactory().uriString(apiurl);
		return uriBuilder;
	}

	@Override
	public <T> T getForObject(UriBuilder uriBuilder, Class<T> clazz, Duration timeout) {
		this.authenticate();
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", this.yoozAuthenticationToken);
		headers.add("applicationId", this.applicationId);

		HttpEntity<String> callData = new HttpEntity<>(headers);
		ParameterizedTypeReference<T> type = new ParameterizedTypeReference<T>() {
		};
		return (T) decorate(() -> restTemplate.exchange(uriBuilder.build(), HttpMethod.GET, callData, type), timeout)
				.getBody();
	}

	@Override
	public <T> T postForObject(UriBuilder uriBuilder, Object data, Class<T> clazz, Duration timeout) {
		this.authenticate();
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", this.yoozAuthenticationToken);
		headers.add("applicationId", this.applicationId);

		HttpEntity<?> callData = new HttpEntity<>(data, headers);
		ParameterizedTypeReference<T> type = new ParameterizedTypeReference<T>() {
		};
		return (T) decorate(() -> restTemplate.exchange(uriBuilder.build(), HttpMethod.POST, callData, type), timeout)
				.getBody();
	}

	public String getYoozAuthenticationToken() {
		return yoozAuthenticationToken;
	}

	public void setYoozAuthenticationToken(String yoozAuthenticationToken) {
		this.yoozAuthenticationToken = yoozAuthenticationToken;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	public ZonedDateTime getTokenRefreshTime() {
		return tokenRefreshTime;
	}

	public void setTokenRefreshTime(ZonedDateTime tokenRefreshTime) {
		this.tokenRefreshTime = tokenRefreshTime;
	}

	public String getYoozEnvironmentHost() {
		return apiurl;
	}

	public void setYoozEnvironmentHost(String yoozEnvironmentHost) {
		this.apiurl = yoozEnvironmentHost;
	}

}
