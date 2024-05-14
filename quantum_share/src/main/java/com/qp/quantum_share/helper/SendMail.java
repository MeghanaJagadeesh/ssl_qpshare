package com.qp.quantum_share.helper;

import java.io.UnsupportedEncodingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.qp.quantum_share.dto.QuantumShareUser;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class SendMail {
	@Autowired
	JavaMailSender mailSender;

	public void sendVerificationEmail(QuantumShareUser userDto) {
		System.out.println("coming");
		MimeMessage message = mailSender.createMimeMessage();
		System.out.println("2");
		MimeMessageHelper helper = new MimeMessageHelper(message);
//		System.out.println("3");
//		String verificationLink = "https://quantumshare.quantumparadigm.in/verify/email?token="
//				+ userDto.getVerificationToken();

		String verificationLink = "http://localhost:7532/quantum-socialshare/user/verify?token="
				+ userDto.getVerificationToken();
		try {
			helper.setFrom("prathyusha10032001@gmail.com", "QuantumShare");
			helper.setTo(userDto.getEmail());
			helper.setSubject("Verify Email");
			helper.setText("<html><body><h1>Hello " + userDto.getFirstName() + "</h1>"
					+ "<p>Please verify your email by clicking the link below:</p>" + "<a href='" + verificationLink
					+ "'>" + verificationLink + "</a>" + "<h3>Thanks and Regards</h3></body></html>", true);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		mailSender.send(message);
	}
}
