package org.bbagisix.user.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class CustomOAuth2User implements OAuth2User {

	private final UserResponse userResponse;

	public CustomOAuth2User(UserResponse userResponse) {
		this.userResponse = userResponse;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return Map.of();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		Collection<GrantedAuthority> collection = new ArrayList<>();
		collection.add(new GrantedAuthority() {
			@Override
			public String getAuthority() {
				return userResponse.getRole();
			}
		});
		return collection;
	}

	@Override
	public String getName() {
		return userResponse.getEmail();
	}

	public String getRole() {
		return userResponse.getRole();
	}

	public String getEmail() {
		return userResponse.getEmail();
	}

	public String getNickname() {
		return userResponse.getNickname();
	}

	public Long getUserId() {
		return userResponse.getUserId();
	}
}
