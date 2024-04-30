package com.qp.quantum_share.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.services.FacebookAccessTokenService;
import com.qp.quantum_share.services.InstagramService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/quantum-share")
public class SocialMediaLoginController {

	@Value("${spring.social.facebook.appId}")
	private String appId;

	@Value("${spring.social.facebook.appSecret}")
	private String appSecret;

	@Value("${spring.social.redirect_uri}")
	private String redirect_uri;

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	FacebookAccessTokenService faceBookAccessTokenService;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	InstagramService instagramService;
//
//	@GetMapping("/login")
//	public RedirectView login() {
//		String authorizationUrl = faceBookAccessTokenService.getAuthorizationUrl();
//		System.out.println(authorizationUrl);
//		return new RedirectView(authorizationUrl);
//	}

	@PostMapping("/facebook/user/verify-token")
	public ResponseEntity<ResponseStructure<String>> callback(@RequestParam(required = false) String code,
			HttpSession session) {
		System.out.println("response coming");
		System.out.println(code);
		QuantumShareUser user = (QuantumShareUser) session.getAttribute("qsuser");
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("Session has expired, Please login");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
//		return new ResponseEntity<ResponseStructure<String>>(HttpStatus.OK);
		if (code == null) {
			structure.setCode(HttpStatus.BAD_REQUEST.value());
			structure.setMessage("Please accept all the permission while login");
			structure.setPlatform("facebook");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.BAD_REQUEST);
		}
		return faceBookAccessTokenService.verifyToken(code,user);
	}

	@PostMapping("/instagram/user/verify-token")
	public ResponseEntity<ResponseStructure<String>> callbackInsta(@RequestParam(required = false) String code, HttpSession session) {
		System.out.println("response coming");
		System.out.println(code);
		
		QuantumShareUser user = (QuantumShareUser) session.getAttribute("qsuser");
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("Session has expired, Please login");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
//		return new ResponseEntity<ResponseStructure<String>>(HttpStatus.OK);
		if (code == null) {
			structure.setCode(HttpStatus.BAD_REQUEST.value());
			structure.setMessage("Please accept all the permission while login");
			structure.setPlatform("instagram");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.BAD_REQUEST);
		}
		return instagramService.verifyToken(code,user);
	}
}
