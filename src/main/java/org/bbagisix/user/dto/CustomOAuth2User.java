package org.bbagisix.user.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class CustomOAuth2User implements OAuth2User {

	private final SignUpResponse signUpResponse;

	public CustomOAuth2User(SignUpResponse signUpResponse) {
		this.signUpResponse = signUpResponse;
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
				return signUpResponse.getRole();
			}
		});
		return collection;
	}

	@Override
	public String getName() {
		return signUpResponse.getEmail();
	}

	public String getRole() {
		return signUpResponse.getRole();
	}

	public String getEmail() {
		return signUpResponse.getEmail();
	}

	public String getNickname() {
		return signUpResponse.getNickname();
	}
}
