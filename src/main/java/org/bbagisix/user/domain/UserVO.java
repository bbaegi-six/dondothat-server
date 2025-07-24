package org.bbagisix.user.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
	private Long id;
	private String email;
	private String password;
	private Long point;
	private String nickname;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	private boolean email_verified;
	private String emailVerificationToken;
	private LocalDateTime emailVerificationTokenExpiry;
}
