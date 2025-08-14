package org.bbagisix.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserResponse {
    private Long userId;
    private String name;
    private String email;
    private String nickname;
    private Integer age;
    private String role;
    private String job;
	private boolean assetConnected;
    private boolean savingConnected;
    private Long tierId;
}