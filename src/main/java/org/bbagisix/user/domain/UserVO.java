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
	private Long userId;
	private String name;
	private String email;
	private String password;
	private String nickname;
	private String socialId;
	private String role;
	private boolean emailVerified;
	private boolean assetConnected;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
