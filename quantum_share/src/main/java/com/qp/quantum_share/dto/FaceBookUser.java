package com.qp.quantum_share.dto;

import java.util.List;

import org.springframework.stereotype.Component;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;

@Entity
@Data
@Component
public class FaceBookUser {

	@Id
	private String fbId;
	private String fbuserId;
	private String fbuserUsername;
	private String firstName;
	private String lastName;
	private String email;
	private String birthday;

	@Column(length = 4000)
	private String pictureUrl;

	@Column(length = 2000)
	private String userAccessToken;

//	@OneToMany(mappedBy = "faceBookUser", cascade = CascadeType.PERSIST) // Optional for persisting page details
	@OneToMany
	private List<FacebookPageDetails> pageDetails;

}
