package org.bbagisix.user.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
	private Long id;
	private String name;
	private String email;
	private String password;
	private Long point;
	private String nickname;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	// 이메일 인증 관련 필드
	private boolean emailVerified;
	private String emailVerificationToken;
	private LocalDateTime emailVerificationTokenExpiry;
}
