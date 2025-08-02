package org.bbagisix.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SignUpResponse {
    private String name;
    private String email;
    private String nickname;
    private String role;
	private boolean assetConnected;
}