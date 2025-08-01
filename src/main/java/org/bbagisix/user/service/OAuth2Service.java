package org.bbagisix.user.service;

public interface OAuth2Service {
	String getGoogleAccessToken(String code);
	// String getNaverAccessToken(String code, String state);
}
