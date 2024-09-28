package com.qp.quantum_share.services;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
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
import com.qp.quantum_share.dto.FaceBookUser;
import com.qp.quantum_share.dto.FacebookPageDetails;
import com.qp.quantum_share.dto.InstagramUser;
import com.qp.quantum_share.dto.LinkedInPageDto;
import com.qp.quantum_share.dto.LinkedInProfileDto;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.dto.SubscriptionDetails;
import com.qp.quantum_share.dto.TelegramUser;
import com.qp.quantum_share.dto.YoutubeUser;
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

	@Value("${default.profile.picture}")
	private String defaultProfile;

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	GenerateId generateId;

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
					applyCredit(user);
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
					sendMail.sendVerificationEmail(user, "signup");
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
				structure.setData(null);
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_ACCEPTABLE);
			}
		}
	}

	private void applyCredit(QuantumShareUser user) {

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
			user.setPassword(SecurePassword.encrypt(user.getPassword(), "123"));
			userDao.saveUser(user);
			String verificationToken = UUID.randomUUID().toString();
			user.setVerificationToken(verificationToken);
			userDao.save(user);
			sendMail.sendVerificationEmail(user, "signup");
			structure.setCode(HttpStatus.CREATED.value());
			structure.setStatus("success");
			structure.setMessage("successfully signedup, please verify your mail.");
			structure.setData(user);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.CREATED);
		}
	}

	public ResponseEntity<ResponseStructure<String>> signInWithGoogle(QuantumShareUser user) {
		List<QuantumShareUser> exUser = userDao.findByEmailOrPhoneNo(user.getEmail(), user.getPhoneNo());
		if (!exUser.isEmpty()) {
			structure.setMessage("Account Already exist");
			structure.setCode(HttpStatus.NOT_ACCEPTABLE.value());
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_ACCEPTABLE);
		} else {
			user.setSignUpDate(LocalDate.now());
			user.setTrial(true);
			user.setVerified(true);
			userDao.save(user);
			String tokenValue = token.generateJWT(user);

			structure.setCode(HttpStatus.CREATED.value());
			structure.setStatus("success");
			structure.setMessage("successfully signedup");
			structure.setData(tokenValue);
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

	public ResponseEntity<ResponseStructure<String>> verifyUpdatedEmail(String token) {
		QuantumShareUser user = userDao.findByVerificationToken(token);
		if (user != null) {
			user.setVerified(true);
			userDao.saveUser(user);

			structure.setCode(HttpStatus.CREATED.value());
			structure.setStatus("success");
			structure.setMessage("email updated successfully, Please login");
			structure.setData(user);

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

	public ResponseEntity<ResponseStructure<String>> accountOverView(int userId, MultipartFile file, String firstname,
			String lastname, String email, Long phoneNo, String company, String jwtToken) {
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}

		String profilepic = null;
		if (file != null) {
			if (!file.getContentType().startsWith("image")) {
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setMessage("Missing or invalid file type");
				structure.setStatus("error");
				structure.setPlatform(null);
				structure.setData(null);
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.UNAUTHORIZED);
			}
			profilepic = uploadProfileToServer.uploadFile(file);
			user.setProfilePic(profilepic);
			userDao.save(user);
		}
		if (firstname != null) {
			user.setFirstName(firstname);
			userDao.save(user);
		}
		if (lastname != null) {
			user.setLastName(lastname);
			userDao.save(user);
		}
		if (phoneNo != null) {
			user.setPhoneNo(phoneNo);
			userDao.save(user);
		}
		if (company != null) {
			user.setCompany(company);
			userDao.save(user);
		}
		if (email != null) {
			String verificationToken = UUID.randomUUID().toString();
			user.setVerificationToken(verificationToken);
			user.setVerified(false);
			user.setEmail(email);
			user.setCredit(3);
			userDao.save(user);
			sendMail.sendVerificationEmail(user, "email_updation");

			structure.setData(null);
			structure.setMessage("Email Updated successfully, please verify your mail");
			structure.setPlatform(null);
			structure.setStatus("success");
			structure.setCode(HttpStatus.OK.value());
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
		}

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

	public int calculateRemainingPackageDays(QuantumShareUser user) {
		LocalDate localDate = LocalDate.now();
		int remainingDays = 0;
		if (user.isTrial()) {
			LocalDate trailDate = user.getSignUpDate();
			if ((freetrail - ChronoUnit.DAYS.between(trailDate, localDate)) > 0) {
				remainingDays = (int) (freetrail - ChronoUnit.DAYS.between(trailDate, localDate));
				return remainingDays;
			} else {
				remainingDays = 0;
				user.setTrial(false);
				user.setCredit(0);
				userDao.saveUser(user);
				return remainingDays;
			}
		} else if (user.getSubscriptionDetails() != null && user.getSubscriptionDetails().isSubscribed()) {
			LocalDate subscriptionDate = user.getSubscriptionDetails().getSubscriptionDate();
			int subscriptiondays = user.getSubscriptionDetails().getSubscriptiondays();
			if ((subscriptiondays - ChronoUnit.DAYS.between(subscriptionDate, localDate)) > 0) {
				remainingDays = (int) (subscriptiondays - ChronoUnit.DAYS.between(subscriptionDate, localDate));
				return remainingDays;
			} else {
				remainingDays = 0;
				SubscriptionDetails subcribedUser = user.getSubscriptionDetails();
				subcribedUser.setSubscribed(false);
				subcribedUser.setSubscriptiondays(0);
				user.setSubscriptionDetails(subcribedUser);
				userDao.save(user);
				return remainingDays;
			}
		} else {
			return 0;
		}
	}

	public ResponseEntity<ResponseStructure<String>> forgetPassword(String email) {
		QuantumShareUser user = userDao.findByEmail(email);
		if (user == null) {
			structure.setData(null);
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("No account found with this email address.");
			structure.setStatus("error");
			structure.setPlatform(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		String verificationToken = UUID.randomUUID().toString();
		user.setVerificationToken(verificationToken);
		userDao.save(user);
		sendMail.sendVerificationEmail(user, "password_reset_request");

		structure.setData(null);
		structure.setMessage("Email has been sent to this mail to chnage your password");
		structure.setPlatform(null);
		structure.setStatus("success");
		structure.setCode(HttpStatus.OK.value());
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);

	}

	public ResponseEntity<ResponseStructure<String>> fetchUserInfo(int userId) {
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		Map<String, Object> usermap = configure.getMap();
		usermap.put("userId", user.getUserId());
		usermap.put("username", user.getFirstName() + " " + user.getLastName());
		usermap.put("email", user.getEmail());
		usermap.put("profilepic", user.getProfilePic());
		usermap.put("socialAccounts", user.getSocialAccounts());
		usermap.put("credit", user.getCredit());

		Map<String, Object> map = configure.getMap();
		map.put("user", usermap);
		map.put("remainingdays", calculateRemainingPackageDays(user));
		System.out.println(map);
		structure.setData(map);
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage(null);
		structure.setStatus("success");
		structure.setPlatform(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);

	}

	public ResponseEntity<ResponseStructure<String>> updatePassword(String password, String token) {
		QuantumShareUser user = userDao.findByVerificationToken(token);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		user.setPassword(SecurePassword.encrypt(password, "123"));
		userDao.save(user);
		sendMail.sendVerificationEmail(user, "password_updation");

		structure.setData(null);
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage("Password has been updated successfully, please login");
		structure.setStatus("success");
		structure.setPlatform(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	// connected platforms

//	public Map<String, Object> fetchConnectedFb(int userId) {
//		QuantumShareUser user = userDao.fetchUser(userId);
//		Map<String, Object> fb = configure.getMap();
//		fb.clear();
//		if (user == null) {
//			return null;
//		}
//		SocialAccounts accounts = user.getSocialAccounts();
//		if (accounts == null || accounts.getFacebookUser() == null) {
//			return null;
//		}
//		FaceBookUser fbuser = accounts.getFacebookUser();
//		if (fbuser != null) {
//			List<FacebookPageDetails> pages = fbuser.getPageDetails();
//			Map<String, Object> pagedata = configure.getMap();
//			pagedata.clear();
//			for (FacebookPageDetails page : pages) {
//				pagedata.put(page.getPageName(), page.getPictureUrl());
//			}
//
//			fb.put("facebookUrl", fbuser.getPictureUrl());
//			fb.put("facebookUsername", fbuser.getFbuserUsername());
//			fb.put("facebookNumberofpages", fbuser.getNoOfFbPages());
//			fb.put("pages_url", pagedata);
//		}
//		return fb;
//	}
//
//	public Map<String, Object> fetchConnectedInsta1(int userId) {
//		QuantumShareUser user = userDao.fetchUser(userId);
//		if (user == null) {
//			return null;
//		}
//		SocialAccounts accounts = user.getSocialAccounts();
//		if (accounts == null || accounts.getInstagramUser() == null) {
//			return null;
//		}
//		InstagramUser instaUser = accounts.getInstagramUser();
//		Map<String, Object> insta = configure.getMap();
//		insta.clear();
//		if (instaUser != null) {
//			String instagramUrl;
//			if (instaUser.getPictureUrl() == null) {
//				instagramUrl = defaultProfile;
//			} else {
//				instagramUrl = instaUser.getPictureUrl();
//			}
//			insta.clear();
//			insta.put("instagramUrl", instagramUrl);
//			insta.put("InstagramUsername", instaUser.getInstaUsername());
//			insta.put("Instagram_follwers_count", instaUser.getFollwersCount());
//		}
//		return insta;
//	}
//
//	public Map<String, Object> fetchConnectedTelegram(int userId) {
//		QuantumShareUser user = userDao.fetchUser(userId);
//		Map<String, Object> telegram = configure.getMap();
//		telegram.clear();
//
//		if (user == null) {
//			return null;
//		}
//		SocialAccounts accounts = user.getSocialAccounts();
//		if (accounts == null || accounts.getTelegramUser() == null) {
//			return null;
//		}
//		TelegramUser telegramUser = accounts.getTelegramUser();
//		if (telegramUser != null) {
//			String imageUrl;
//			if (telegramUser.getTelegramProfileUrl() == null) {
//				imageUrl = defaultProfile;
//			} else {
//				imageUrl = telegramUser.getTelegramProfileUrl();
//			}
//			telegram.put("telegramChatId", telegramUser.getTelegramChatId());
//			telegram.put("telegramGroupName", telegramUser.getTelegramGroupName());
//			telegram.put("telegramProfileUrl", imageUrl);
//			telegram.put("telegramGroupMembersCount", telegramUser.getTelegramGroupMembersCount());
//		}
//
//		return telegram;
//	}
//
//	public Map<String, Object> fetchLinkedIn(int userId) {
//		QuantumShareUser user = userDao.fetchUser(userId);
//		Map<String, Object> linkedIn = configure.getMap();
//		linkedIn.clear();
//		if (user == null) {
//			return null;
//		}
//		SocialAccounts accounts = user.getSocialAccounts();
//		if (accounts == null || accounts.getLinkedInProfileDto() == null) {
//			return null;
//		}
//		LinkedInProfileDto linkedInUser = accounts.getLinkedInProfileDto();
//		String imageUrl = null;
//		if (linkedInUser.getLinkedinProfileURN() != null) {
//			if (linkedInUser.getLinkedinProfileImage() == null) {
//				imageUrl = defaultProfile;
//			} else {
//				imageUrl = linkedInUser.getLinkedinProfileImage();
//			}
//			linkedIn.put("linkedInProfilePic", imageUrl);
//			linkedIn.put("linkedInUserName", linkedInUser.getLinkedinProfileUserName());
//		} else if (linkedInUser.getPages().get(0).getLinkedinPageURN() != null) {
//			LinkedInPageDto linkedInPage = linkedInUser.getPages().get(0);
//			if (linkedInPage.getLinkedinPageImage() == null) {
//				imageUrl = defaultProfile;
//			} else {
//				imageUrl = linkedInPage.getLinkedinPageImage();
//			}
//			linkedIn.put("linkedInProfilePic", imageUrl);
//			linkedIn.put("linkedInUserName", linkedInPage.getLinkedinPageName());
//			linkedIn.put("linkedInFollowersCount", linkedInPage.getLinkedinPageFollowers());
//		}
//		return linkedIn;
//	}
//
//	public Map<String, Object> fetchConnectedYoutube(int userId) {
//		Map<String, Object> youtube = configure.getMap();
//		youtube.clear();
//		QuantumShareUser user = userDao.fetchUser(userId);
//		if (user == null) {
//			return null;
//		}
//		SocialAccounts accounts = user.getSocialAccounts();
//		if (accounts == null || accounts.getYoutubeUser() == null) {
//			return null;
//		}
//		YoutubeUser youTubeUser = accounts.getYoutubeUser();
//		if (youTubeUser != null) {
//			String youTubeUrl;
//			if (youTubeUser.getChannelImageUrl() == null) {
//				youTubeUrl = defaultProfile;
//			} else {
//				youTubeUrl = youTubeUser.getChannelImageUrl();
//			}
//			youtube.clear();
//			youtube.put("youtubeUrl", youTubeUrl);
//			youtube.put("youtubeChannelName", youTubeUser.getChannelName());
//			youtube.put("youtubeSubscriberCount", youTubeUser.getSubscriberCount());
//		}
//		return youtube;
//	}
//
//	public ResponseEntity<ResponseStructure<String>> ConnectedSocialMediaPlatform(int userId) {
//		Map<String, Object> fb = fetchConnectedFb(userId);
//		Map<String, Object> insta = fetchConnectedInsta1(userId);
//		Map<String, Object> tele = fetchConnectedTelegram(userId);
//		Map<String, Object> youtube = fetchConnectedYoutube(userId);
//		Map<String, Object> linkedin = fetchLinkedIn(userId);
//		Map<String, Object> map = configure.getMap();
//		map.clear();
//		map.put("facebook", fb);
//		map.put("instagram", insta);
//		map.put("telegram", tele);
//		map.put("youtube", youtube);
//		map.put("linkedin", linkedin);
//		structure.setCode(HttpStatus.OK.value());
//		structure.setMessage(null);
//		structure.setPlatform(null);
//		structure.setStatus("success");
//		structure.setData(map);
//		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
//	}

	public ResponseEntity<ResponseStructure<String>> fetchConnectedFb(int userId) {
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getFacebookUser() == null) {
			structure.setCode(119);
			structure.setMessage("user has not connected facebook platforms");
			structure.setPlatform("facebook");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		FaceBookUser fbuser = accounts.getFacebookUser();
		Map<String, Object> data = configure.getMap();
		data.clear();
		if (fbuser != null) {
			List<FacebookPageDetails> pages = fbuser.getPageDetails();
			Map<String, Object> pagedata = configure.getMap();
			pagedata.clear();
			for (FacebookPageDetails page : pages) {
				pagedata.put(page.getPageName(), page.getPictureUrl());
			}
			Map<String, Object> fb = configure.getMap();
			fb.clear();
			fb.put("facebookUrl", fbuser.getPictureUrl());
			fb.put("facebookUsername", fbuser.getFbuserUsername());
			fb.put("facebookNumberofpages", fbuser.getNoOfFbPages());
			fb.put("pages_url", pagedata);
			data.put("facebook", fb);
		}
		System.out.println(data);
		structure.setData(data);
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage(null);
		structure.setStatus("success");
		structure.setPlatform(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> fetchConnectedInsta1(int userId) {
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getInstagramUser() == null) {
			structure.setCode(119);
			structure.setMessage("user has not connected Instagram platforms");
			structure.setPlatform("instagram");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		InstagramUser instaUser = accounts.getInstagramUser();
		Map<String, Object> data = configure.getMap();
		data.clear();
		if (instaUser != null) {
			Map<String, Object> insta = configure.getMap();
			String instagramUrl;
			if (instaUser.getPictureUrl() == null) {
				instagramUrl = defaultProfile;
			} else {
				instagramUrl = instaUser.getPictureUrl();
			}
			insta.clear();
			insta.put("instagramUrl", instagramUrl);
			insta.put("InstagramUsername", instaUser.getInstaUsername());
			insta.put("Instagram_follwers_count", instaUser.getFollwersCount());
			data.put("instagram", insta);
		}
		structure.setData(data);
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage(null);
		structure.setStatus("success");
		structure.setPlatform(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> fetchConnectedTelegram(int userId) {
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getTelegramUser() == null) {
			structure.setCode(119);
			structure.setMessage("user has not connected telegram platforms");
			structure.setPlatform("telegram");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		TelegramUser telegramUser = accounts.getTelegramUser();
		Map<String, Object> data = configure.getMap();
		data.clear();
		if (telegramUser != null) {
			Map<String, Object> telegram = configure.getMap();
			telegram.clear();
			String imageUrl;
			if (telegramUser.getTelegramProfileUrl() == null) {
				imageUrl = defaultProfile;
			} else {
				imageUrl = telegramUser.getTelegramProfileUrl();
			}
			telegram.put("telegramChatId", telegramUser.getTelegramChatId());
			telegram.put("telegramGroupName", telegramUser.getTelegramGroupName());
			telegram.put("telegramProfileUrl", imageUrl);
			telegram.put("telegramGroupMembersCount", telegramUser.getTelegramGroupMembersCount());
			data.put("telegram", telegram);
		}
		structure.setData(data);
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage(null);
		structure.setStatus("success");
		structure.setPlatform(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

//	public ResponseEntity<ResponseStructure<String>> fetchLinkedIn(int userId) {
//		QuantumShareUser user = userDao.fetchUser(userId);
//		if (user == null) {
//			structure.setCode(HttpStatus.NOT_FOUND.value());
//			structure.setMessage("user doesn't exists, please login");
//			structure.setStatus("error");
//			structure.setData(null);
//			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
//		}
//		SocialAccounts accounts = user.getSocialAccounts();
//		if (accounts == null || accounts.getLinkedInProfileDto() == null) {
//			structure.setCode(119);
//			structure.setMessage("user has not connected linkedIn platforms");
//			structure.setPlatform("linkedIn");
//			structure.setStatus("error");
//			structure.setData(null);
//			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
//		}
//		LinkedInProfileDto linkedInUser = accounts.getLinkedInProfileDto();
//		Map<String, Object> data = new HashMap<>();  // Create a new map for each request
//		Map<String, Object> linkedIn = new HashMap<>();
//		data.clear();
//		linkedIn.clear();
//		String imageUrl = null;
//		if (linkedInUser.getLinkedinProfileURN() != null) {
//			if (linkedInUser.getLinkedinProfileImage() == null) {
//				imageUrl = defaultProfile;
//			} else {
//				imageUrl = linkedInUser.getLinkedinProfileImage();
//			}
//			linkedIn.put("linkedInProfilePic", imageUrl);
//			linkedIn.put("linkedInUserName", linkedInUser.getLinkedinProfileUserName());
//			data.put("linkedIn", linkedIn);
//		} else if (linkedInUser.getPages().get(0).getLinkedinPageURN() != null) {
//			LinkedInPageDto linkedInPage = linkedInUser.getPages().get(0);
//			if (linkedInPage.getLinkedinPageImage() == null) {
//				imageUrl = defaultProfile;
//			} else {
//				imageUrl = linkedInPage.getLinkedinPageImage();
//			}
//			System.out.println(imageUrl);
//			linkedIn.put("linkedInProfilePic", imageUrl);
//			linkedIn.put("linkedInUserName", linkedInPage.getLinkedinPageName());
//			linkedIn.put("linkedInFollowersCount", linkedInPage.getLinkedinPageFollowers());
//			data.put("linkedIn", linkedIn);
//		}
//
//		structure.setData(data);
//		structure.setCode(HttpStatus.OK.value());
//		structure.setMessage(null);
//		structure.setStatus("success");
//		structure.setPlatform("linkedIn");
//		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
//
//	}

	public ResponseEntity<ResponseStructure<String>> fetchLinkedIn(int userId) {
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("User doesn't exist, please log in");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}

		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || (!accounts.isLinkedInPagePresent() && accounts.getLinkedInProfileDto() == null)) {
			structure.setCode(119);
			structure.setMessage("User has not connected LinkedIn platforms");
			structure.setPlatform("LinkedIn");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}

		Map<String, Object> data = new HashMap<>();
		Map<String, Object> linkedIn = new HashMap<>();

		// Map pages if they exist
		if (accounts.isLinkedInPagePresent()) {
			LinkedInPageDto page = accounts.getLinkedInPages(); // Assuming single page example
			Map<String, Object> pageData = new HashMap<>();
			pageData.put("linkedInPageURN", page.getLinkedinPageURN());
			pageData.put("linkedInPageName", page.getLinkedinPageName());
			pageData.put("linkedInPageImage", page.getLinkedinPageImage());
			pageData.put("linkedInFollowersCount", page.getLinkedinPageFollowers());
			linkedIn.put("page", pageData);
		} else {
			LinkedInProfileDto linkedInUser = accounts.getLinkedInProfileDto();
			String imageUrl = (linkedInUser.getLinkedinProfileImage() == null) ? defaultProfile
					: linkedInUser.getLinkedinProfileImage();
			linkedIn.put("linkedInProfilePic", imageUrl);
			linkedIn.put("linkedInUserName", linkedInUser.getLinkedinProfileUserName());

		}

		data.put("linkedIn", linkedIn);
		structure.setData(data);
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage(null);
		structure.setStatus("success");
		structure.setPlatform("LinkedIn");

		return new ResponseEntity<>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> fetchConnectedYoutube(int userId) {
		QuantumShareUser user = userDao.fetchUser(userId);
		if (user == null) {
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setMessage("user doesn't exists, please login");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getYoutubeUser() == null) {
			structure.setCode(119);
			structure.setMessage("user has not connected youtube platforms");
			structure.setPlatform("youtube");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
		}
		YoutubeUser youTubeUser = accounts.getYoutubeUser();
		Map<String, Object> data = configure.getMap();
		data.clear();
		if (youTubeUser != null) {
			Map<String, Object> youtube = configure.getMap();
			String youTubeUrl;
			if (youTubeUser.getChannelImageUrl() == null) {
				youTubeUrl = defaultProfile;
			} else {
				youTubeUrl = youTubeUser.getChannelImageUrl();
			}
			youtube.clear();
			youtube.put("youtubeUrl", youTubeUrl);
			youtube.put("youtubeChannelName", youTubeUser.getChannelName());
			youtube.put("youtubeSubscriberCount", youTubeUser.getSubscriberCount());
			data.put("youtube", youtube);
		}
		structure.setData(data);
		structure.setCode(HttpStatus.OK.value());
		structure.setMessage(null);
		structure.setStatus("success");
		structure.setPlatform("youtube");
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

}
