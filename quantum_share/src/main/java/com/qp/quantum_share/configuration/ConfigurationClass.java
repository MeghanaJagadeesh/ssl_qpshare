package com.qp.quantum_share.configuration;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.qp.quantum_share.dto.FacebookPageDetails;
import com.qp.quantum_share.response.ErrorResponse;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.response.ResponseWrapper;
import com.qp.quantum_share.response.SuccessResponse;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Version;

@Component
public class ConfigurationClass {
	@Bean
	public HttpHeaders httpHeaders() {
		return new HttpHeaders();
	}

	@Bean
	@Lazy
	public HttpEntity<String> getHttpEntity(String jsonString, HttpHeaders headers) {
		return new HttpEntity<>(jsonString, headers);
	}

	@Bean
	@Lazy
	public HttpEntity<String> getHttpEntity(HttpHeaders headers) {
		return new HttpEntity<>(headers);
	}

	@Bean
	@Lazy
	public Map<String, Long> getMap() {
		return new HashMap<String, Long>();
	}

	@Bean
	public RestTemplate getRestTemplate() {
		return new RestTemplate();
	}

	@Bean
	@Lazy
	public FacebookPageDetails pageDetails() {
		return new FacebookPageDetails();
	}

	@Bean
	@Lazy
	public FacebookClient getFacebookClient(String accessToken) {
		return new DefaultFacebookClient(accessToken, Version.LATEST);
	}

	@Bean
	public ObjectMetadata getMetaObject() {
		return new ObjectMetadata();
	}

	@Bean
	@Lazy
	public ResponseWrapper getResponseWrapper(ResponseStructure<String> structure) {
		return new ResponseWrapper(structure);
	}

	@Bean
	@Lazy
	public ResponseWrapper getResponseWrapper(SuccessResponse successResponse) {
		return new ResponseWrapper(successResponse);
	}

	@Bean
	@Lazy
	public ResponseWrapper getResponseWrapper(ErrorResponse errorResponse) {
		return new ResponseWrapper(errorResponse);
	}

//	@Bean
//	public JsonObject getJsonObject(String json)
//	{
//		return new JsonObject(json);
//	}

//	@Bean
//	public JavaMailSender mailSender() {
//		return new JavaMailSenderImpl();
//	}

}
