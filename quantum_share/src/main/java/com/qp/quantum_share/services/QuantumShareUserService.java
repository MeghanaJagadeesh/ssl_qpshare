package com.qp.quantum_share.services;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.SubscriptionDetails;
import com.qp.quantum_share.helper.GenerateId;
import com.qp.quantum_share.helper.JwtToken;
import com.qp.quantum_share.helper.SecurePassword;
import com.qp.quantum_share.helper.SendMail;
import com.qp.quantum_share.helper.UploadProfileToServer;
import com.qp.quantum_share.response.ResponseStructure;

@Service
public class QuantumShareUserService {

	@Value("${quantumshare.freetrail}")
	private int freetrail;

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	GenerateId generateId;

//	@Autowired
//	QuantumShareUser user;

	@Autowired
	SendMail sendMail;

	@Autowired
	JwtToken token;

	@Autowired
	ConfigurationClass configure;

	@Autowired
	SubscriptionDetails subscriptionDetails;

	@Autowired
	UploadProfileToServer uploadProfileToServer;

	public ResponseEntity<ResponseStructure<String>> login(String emph, String password) {
		long mobile = 0;
		String email = null;
		try {
			mobile = Long.parseLong(emph);
		} catch (NumberFormatException e) {
			email = emph;
		}
		List<QuantumShareUser> users = userDao.findByEmailOrPhoneNo(email, mobile);
		if (users.isEmpty()) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("Invalid email or mobile");
			structure.setStatus("success");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		} else {
			QuantumShareUser user = users.get(0);
			if (SecurePassword.decrypt(user.getPassword(), "123").equals(password)) {
				if (user.isVerified()) {
					String tokenValue = token.generateJWT(user);
					structure.setCode(HttpStatus.OK.value());
					structure.setMessage("Login Successful");
					structure.setStatus("success");
					structure.setData(tokenValue);
					return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);

				} else {
					String verificationToken = UUID.randomUUID().toString();
					user.setVerificationToken(verificationToken);
					userDao.save(user);
					sendMail.sendVerificationEmail(user);
					structure.setCode(HttpStatus.NOT_ACCEPTABLE.value());
					structure.setMessage("please verify your email, email has been sent.");
					structure.setStatus("error");
					structure.setData(user);
					return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_ACCEPTABLE);
				}
			} else {
				structure.setCode(HttpStatus.NOT_ACCEPTABLE.value());
				structure.setMessage("Invalid Password");
				structure.setStatus("error");
				structure.setData(user);
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_ACCEPTABLE);
			}
		}
	}

	public ResponseEntity<ResponseStructure<String>> userSignUp(QuantumShareUser user) {
		List<QuantumShareUser> exUser = userDao.findByEmailOrPhoneNo(user.getEmail(), user.getPhoneNo());
		if (!exUser.isEmpty()) {
			structure.setMessage("Account Already exist");
			structure.setCode(HttpStatus.NOT_ACCEPTABLE.value());
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_ACCEPTABLE);
		} else {

//			String userId = generateId.generateuserId();
//			System.out.println("userId : " + userId);
//			user.setUserId(userId);
			user.setPassword(SecurePassword.encrypt(user.getPassword(), "123"));
			userDao.saveUser(user);

			String verificationToken = UUID.randomUUID().toString();
			user.setVerificationToken(verificationToken);
			userDao.save(user);

			sendMail.sendVerificationEmail(user);

			structure.setCode(HttpStatus.CREATED.value());
			structure.setStatus("success");
			structure.setMessage("successfully signedup, please verify your mail.");
			structure.setData(user);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.CREATED);
		}
	}

	public ResponseEntity<ResponseStructure<String>> verifyEmail(String token) {
		QuantumShareUser user = userDao.findByVerificationToken(token);
		if (user != null) {
			user.setVerified(true);
			user.setSignUpDate(LocalDate.now());
			user.setTrial(true);
			user.setCredit(3);
			Map<String, Object> map = configure.getMap();
			map.put("remainingdays", freetrail);
			map.put("user", user);
			userDao.saveUser(user);

			structure.setCode(HttpStatus.CREATED.value());
			structure.setStatus("success");
			structure.setMessage("successfully signedup");
			structure.setData(map);
			
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.CREATED);
		} else {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("Email verification failed... ");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.BAD_REQUEST);
		}
	}

	public ResponseEntity<ResponseStructure<String>> accountOverView(int userId) {
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		Map<String, Object> map = configure.getMap();
		map.clear();
		map.put("name", user.getFirstName() + " " + user.getLastName());
		map.put("company_name", user.getCompany());
		map.put("email", user.getEmail());
		map.put("mobile", user.getPhoneNo());
		map.put("profile_pic", user.getProfilePic());

		structure.setCode(HttpStatus.OK.value());
		structure.setData(map);
		structure.setStatus("success");
		structure.setMessage(null);
		structure.setPlatform(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> accountOverView(int userId, MultipartFile file) {
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		String profilepic = uploadProfileToServer.uploadFile(file);
		user.setProfilePic(profilepic);
		userDao.save(user);
		structure.setCode(HttpStatus.OK.value());
		Map<String, Object> map = configure.getMap();
		map.clear();
		map.put("name", user.getFirstName() + " " + user.getLastName());
		map.put("company_name", user.getCompany());
		map.put("email", user.getEmail());
		map.put("mobile", user.getPhoneNo());
		map.put("profile_pic", profilepic);
		structure.setData(map);
		structure.setMessage("Updated successfully");
		structure.setPlatform(null);
		structure.setStatus("success");
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> calculateRemainingPackageDays(int userId) {
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please signup");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		LocalDate localDate = LocalDate.now();
		int remainingDays = 0;
		if (user.isTrial()) {
			LocalDate trailDate = user.getSignUpDate();
			if ((freetrail - ChronoUnit.DAYS.between(trailDate, localDate)) > 0) {
				remainingDays = (int) (freetrail - ChronoUnit.DAYS.between(trailDate, localDate));
				user.setCredit(3);
				userDao.save(user);
				Map<String, Object> map = configure.getMap();
				map.clear();
				map.put("remainingdays", remainingDays);
				map.put("credits", user.getCredit());

				structure.setCode(HttpStatus.OK.value());
				structure.setMessage("remaining access in days : " + remainingDays);
				structure.setData(map);
				structure.setStatus("success");
				structure.setPlatform(null);
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
			} else {
				remainingDays = 0;
				user.setTrial(false);
				user.setCredit(0);
				userDao.saveUser(user);
				Map<String, Object> map = configure.getMap();
				map.clear();
				map.put("remainingdays", remainingDays);
				map.put("credits", user.getCredit());

				structure.setCode(HttpStatus.NOT_EXTENDED.value());
				structure.setMessage("remaining access in days : " + remainingDays);
				structure.setData(map);
				structure.setStatus("success");
				structure.setPlatform(null);
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);

			}
		} else if (user.getSubscriptionDetails() != null && user.getSubscriptionDetails().isSubscribed()) {
			LocalDate subscriptionDate = user.getSubscriptionDetails().getSubscriptionDate();
			int subscriptiondays = user.getSubscriptionDetails().getSubscriptiondays();
			if ((subscriptiondays - ChronoUnit.DAYS.between(subscriptionDate, localDate)) > 0) {
				remainingDays = (int) (subscriptiondays - ChronoUnit.DAYS.between(subscriptionDate, localDate));
				Map<String, Object> map = configure.getMap();
				map.clear();
				map.put("remainingdays", remainingDays);

				structure.setCode(HttpStatus.OK.value());
				structure.setMessage("remaining access in days : " + remainingDays);
				structure.setData(map);
				structure.setStatus("success");
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.FOUND);

			} else {
				remainingDays = 0;
				SubscriptionDetails subcribedUser = user.getSubscriptionDetails();
				subcribedUser.setSubscribed(false);
				subcribedUser.setSubscriptiondays(0);
				user.setSubscriptionDetails(subcribedUser);
				userDao.save(user);
				Map<String, Object> map = configure.getMap();
				map.clear();
				map.put("remainingdays", remainingDays);

				structure.setCode(HttpStatus.NOT_EXTENDED.value());
				structure.setMessage("remaining access in days : " + remainingDays);
				structure.setData(map);
				structure.setStatus("error");
				structure.setPlatform(null);
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_EXTENDED);

			}
		} else {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("Package has been expired!! Please Subscribe Your package.");
			structure.setData(remainingDays);
			structure.setStatus("error");
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
	}
}
