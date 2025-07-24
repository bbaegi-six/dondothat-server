package org.bbagisix.user.dto;

import javax.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@NotBlank
@AllArgsConstructor
public class SignUpResponse {
	private Long id;
	private String name;
	private String email;
	private String nickname;
	private boolean emailVerified;
	private String message;
}


