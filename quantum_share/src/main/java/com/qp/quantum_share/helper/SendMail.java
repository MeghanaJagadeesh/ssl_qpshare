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
public class SendMail 
{
	@Autowired
	JavaMailSender mailSender;
	
	public void sendVerificationEmail(QuantumShareUser userDto) {
		System.out.println("coming");
		MimeMessage message = mailSender.createMimeMessage();
		System.out.println("2");
		MimeMessageHelper helper = new MimeMessageHelper(message);
		System.out.println("3");
		String verificationLink = "http://localhost:7532/quantum-socialshare/user/verify?token=" + userDto.getVerificationToken();
		try {
			System.out.println("5");
			helper.setFrom("demodem866@gmail.com", "quantumshare");
			System.out.println("6");
			helper.setTo(userDto.getEmail());
			System.out.println("7");
			helper.setSubject("Verify Email");
			System.out.println("8");
			helper.setText("<html><body><h1>Hello " + userDto.getFirstName() + "</h1>"
			            + "<p>Please verify your email by clicking the link below:</p>"
			            + "<a href='" + verificationLink + "'>" + verificationLink + "</a>"
			            + "<h3>Thanks and Regards</h3></body></html>", true);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		 mailSender.send(message);
	}
}
