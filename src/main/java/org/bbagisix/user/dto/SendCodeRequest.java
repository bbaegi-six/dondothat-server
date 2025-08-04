package org.bbagisix.user.dto;

import javax.validation.constraints.Email;

import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class SendCodeRequest {
	@NotBlank
	@Email
	private String email;
}
