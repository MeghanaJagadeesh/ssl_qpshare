package com.qp.quantum_share.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.services.QuantumShareUserService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/quantum-socialshare/user")
public class QuantumShareUserController {

	@Autowired
	QuantumShareUserService quantumShareUserService;

	@PostMapping("/login")
	public ResponseEntity<ResponseStructure<String>> userLogin(@RequestParam String emph, @RequestParam String password,
			HttpSession session) {
		System.out.println("emph" + emph + " password : " + password);
		return quantumShareUserService.login(emph, password, session);
	}

	@PostMapping("/signup")
	public ResponseEntity<ResponseStructure<String>> signup(@RequestBody QuantumShareUser userDto) {
		System.out.println("userDto" + userDto);
		return quantumShareUserService.userSignUp(userDto);
	}

//	@GetMapping("/access/remainingdays")
//	public ResponseEntity<PackageResponse> userRemainingDays(@RequestBody User user) {
//		return quantumShareUserService.calculateRemainingPackageDays(user);
//	}

	@GetMapping("/verify")
	public ResponseEntity<ResponseStructure<String>> verifyEmail(@RequestParam("token") String token) {
		return quantumShareUserService.verifyEmail(token);
	}

}
