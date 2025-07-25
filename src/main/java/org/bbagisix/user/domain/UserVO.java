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
	private boolean emailVerified;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
