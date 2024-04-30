package com.qp.quantum_share.dto;

import org.springframework.stereotype.Component;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
@Component
public class SocialAccounts {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int socialAccountId;
	private String fbId;
	private String instaId;

}
