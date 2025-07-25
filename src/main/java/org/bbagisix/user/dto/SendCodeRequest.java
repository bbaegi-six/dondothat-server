package org.bbagisix.user.dto;

import javax.validation.constraints.Email;

import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class SendCodeRequest {
	@NotBlank
	@Email
	private String email;
}
