package org.bbagisix.user.dto;

import javax.validation.constraints.Email;

import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequest {
	@NotBlank(message = "이름은 필수입니다.")
	private String name;

	@NotBlank(message = "닉네임은 필수입니다.")
	private String nickname;

	@NotBlank(message = "비밀번호는 필수입니다.")
	private String password;

	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	private String email;

	@NotBlank(message = "인증 코드는 필수입니다.")
	private String verificationCode;
}
