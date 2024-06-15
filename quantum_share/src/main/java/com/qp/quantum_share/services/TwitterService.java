package com.qp.quantum_share.services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.dto.TwitterUser;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.response.ResponseStructure;
import com.twitter.clientlib.ApiResponse;

@Service
public class TwitterService {

	@Value("${twitter.client_id}")
	private String client_id;

	@Value("${twitter.redirect_uri}")
	private String redirect_uri;

	@Value("${twitter.code_challenge_method}")
	private String code_challenge_method;

	@Value("${twitter.scope}")
	private String scope;

	@Value("${twitter.state}")
	private String state;

	@Value("${twitter.code_challenge}")
	private String code_challenge;

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	HttpHeaders headers;

	@Autowired
	ConfigurationClass configurationClass;

	@Autowired
	MultiValueMap<String, Object> multiValueMap;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	TwitterUser twitterUser;

	@Autowired
	SocialAccounts socialAccounts;

	@Autowired
	QuantumShareUserDao userDao;


	public ResponseEntity<ResponseStructure<String>> getAuthorizationUrl(QuantumShareUser user) {
		String apiUrl = "https://twitter.com/i/oauth2/authorize";
		String ouath = apiUrl + "?response_type=code&client_id=" + client_id + "&redirect_uri=" + redirect_uri
				+ "&scope=" + scope + "&state=kkrpoiuytr&code_challenge=" + code_challenge + "&code_challenge_method="
				+ code_challenge_method;
		System.out.println(ouath);
		structure.setCode(HttpStatus.OK.value());
		structure.setStatus("success");
		structure.setMessage("oauth_url generated successfully");
		structure.setPlatform(null);
		structure.setData(ouath);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> verifyToken(String code, QuantumShareUser user) {
		try {
			String url = "https://api.twitter.com/2/oauth2/token";

			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			multiValueMap.add("code", code);
			multiValueMap.add("grant_type", "authorization_code");
			multiValueMap.add("redirect_uri", "https://quantumparadigm.in/");
			multiValueMap.add("code_verifier", "challenge");
			multiValueMap.add("client_id", "aWRKTjQ5ZVFqZzRSUnRLeEdVRU46MTpjaQ");

			HttpEntity<MultiValueMap<String, Object>> httpRequest = new HttpEntity<>(multiValueMap, headers);
			System.out.println("request : " + httpRequest + "   -   " + httpRequest.toString());
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, httpRequest, String.class);
			System.out.println("response  :  " + response);
			if (response.getStatusCode().is2xxSuccessful()) {
				JsonNode responseBody = objectMapper.readTree(response.getBody());
				System.out.println("success");
				String access_token = responseBody.get("access_token").asText();
				System.out.println(access_token);
				String tweetUser = fetchUser(access_token);
				return saveTwitterUser(tweetUser, user, access_token);
			} else {
				structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				structure.setData(null);
				structure.setMessage("Something went wrong!!");
				structure.setPlatform(null);
				structure.setStatus("error");
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
			}

		} catch (JsonProcessingException e) {
			throw new CommonException(e.getMessage());
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}

	}

	private ResponseEntity<ResponseStructure<String>> saveTwitterUser(String tweetUser, QuantumShareUser user, String access_token) {
		try {
			JsonNode rootNode = objectMapper.readTree(tweetUser);
			SocialAccounts accounts = user.getSocialAccounts();
			if (accounts == null) {
				twitterUser.setTwitterUserId(rootNode.get("data").get("id").asLong());
				twitterUser.setAccess_token(access_token);
				twitterUser.setName(rootNode.get("data").get("name").asText());
				twitterUser.setUserName(rootNode.get("data").get("username").asText());
				twitterUser.setPicture_url(rootNode.get("data").get("profile_image_url").asText());
				twitterUser
						.setFollower_count(rootNode.get("data").get("public_metrics").get("followers_count").asInt());

				socialAccounts.setTwitterUser(twitterUser);
				user.setSocialAccounts(socialAccounts);
			} else if (accounts.getTwitterUser() == null) {
				twitterUser.setAccess_token(access_token);
				twitterUser.setTwitterUserId(rootNode.get("data").get("id").asLong());
				twitterUser.setName(rootNode.get("data").get("name").asText());
				twitterUser.setUserName(rootNode.get("data").get("username").asText());
				twitterUser.setPicture_url(rootNode.get("data").get("profile_image_url").asText());
				twitterUser
						.setFollower_count(rootNode.get("data").get("public_metrics").get("followers_count").asInt());
				accounts.setTwitterUser(twitterUser);
				user.setSocialAccounts(accounts);
			} else {
				TwitterUser exUser = accounts.getTwitterUser();
				exUser.setTwitterUserId(rootNode.get("data").get("id").asLong());
				exUser.setAccess_token(access_token);
				exUser.setName(rootNode.get("data").get("name").asText());
				exUser.setUserName(rootNode.get("data").get("username").asText());
				exUser.setPicture_url(rootNode.get("data").get("profile_image_url").asText());
				exUser.setFollower_count(rootNode.get("data").get("public_metrics").get("followers_count").asInt());
				accounts.setTwitterUser(exUser);
				user.setSocialAccounts(accounts);
			}
			userDao.save(user);

			structure.setCode(HttpStatus.OK.value());
			structure.setStatus("success");
			structure.setMessage("twitter connected successfully");
			structure.setPlatform("twitter");

			TwitterUser tuser = user.getSocialAccounts().getTwitterUser();
			Map<String, Object> map = configurationClass.getMap();
			map.put("username", tuser.getUserName());
			map.put("follower_count", tuser.getFollower_count());
			map.put("picture_url", tuser.getPicture_url());

			structure.setData(map);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);

		} catch (NullPointerException e) {
			throw new CommonException(e.getMessage());
		} catch (JsonMappingException e) {
			throw new CommonException(e.getMessage());
		} catch (JsonProcessingException e) {
			throw new CommonException(e.getMessage());
		}
	}

	private String fetchUser(String access_token) {
		System.out.println("fetch user acc_tok : " + access_token);
		try {
			String apiUrl = "https://api.twitter.com/2/users/me?user.fields=id,name,profile_image_url,username,public_metrics";
			headers.setBearerAuth(access_token);
//			String requestBody = "user.fields=id,name,profile_image_url,username,public_metrics";
			HttpEntity<String> httpRequest = configurationClass.getHttpEntity(headers);
			ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, httpRequest, String.class);
			if (response.getStatusCode().is2xxSuccessful()) {
				return response.getBody();
			} else {
				return null;
			}
		} catch (NullPointerException exception) {
			throw new CommonException(exception.getMessage());
		}
	}
	
	public void postTweet(String tweet) {
//		ApiResponse<T>
	}
}


