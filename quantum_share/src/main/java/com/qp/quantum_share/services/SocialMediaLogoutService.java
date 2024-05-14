package com.qp.quantum_share.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.qp.quantum_share.dao.FaceBookPageDao;
import com.qp.quantum_share.dao.FacebookUserDao;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dao.SocialAccountDao;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.dto.SocialAccounts;
import com.qp.quantum_share.response.ResponseStructure;

@Service
public class SocialMediaLogoutService {

	@Autowired
	FacebookUserDao facebookUserDao;

	@Autowired
	FaceBookPageDao pageDao;

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	SocialAccountDao accountDao;

	@Autowired
	QuantumShareUserDao userDao;

	public ResponseEntity<ResponseStructure<String>> disconnectFacebook(QuantumShareUser user) {
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getFacebookUser() == null) {
			structure.setCode(404); // Or a custom code for Facebook not linked
			structure.setMessage("Facebook account not linked to this user");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("facebook");
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}

////        pageDao.deletePage(fbUser.getPageDetails());
//		List<FacebookPageDetails> list = fbUser.getPageDetails();
//		facebookUserDao.deleteFbUser(fbUser);
//		user.setSocialAccounts(accounts);
//		userDao.saveUser(user);
//		pageDao.deletePage(list);
		accounts.getFacebookUser().setPageDetails(null);
		accounts.setFacebookUser(null);
		user.setSocialAccounts(accounts);
		userDao.save(user);

		structure.setCode(HttpStatus.OK.value());
		structure.setMessage("Facebook Disconnected Successfully");
		structure.setPlatform("facebook");
		structure.setStatus("success");
		structure.setData(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> disconnectInstagram(QuantumShareUser user) {
		SocialAccounts accounts = user.getSocialAccounts();
		if (accounts == null || accounts.getInstagramUser() == null) {
			structure.setCode(404); // Or a custom code for Facebook not linked
			structure.setMessage("Instagram account not linked to this user");
			structure.setStatus("error");
			structure.setData(null);
			structure.setPlatform("instagram");
			return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
		}

		accounts.setInstagramUser(null);
		user.setSocialAccounts(accounts);
		userDao.save(user);

		structure.setCode(HttpStatus.OK.value());
		structure.setMessage("Instagram Disconnected Successfully");
		structure.setPlatform("instagram");
		structure.setStatus("success");
		structure.setData(null);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);

	}
}
