package org.bbagisix.user.service;


import org.bbagisix.user.dto.GoogleTokenResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class OAuth2ServiceImpl implements OAuth2Service {

	private final RestTemplate restTemplate;

	@Value("${GOOGLE_CLIENT_ID}")
	private String googleClientId;
	@Value("${GOOGLE_CLIENT_SECRET}")
	private String googleClientSecret;
	@Value("${BASE_URL}")
	private String baseUrl;

	// @Value("{NAVER_CLIENT_ID}")
	// private String naverClientId;
	// @Value("{NAVER_CLIENT_SECRET}")
	// private String naverClientSecret;

	private static final String GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token";
	// private static final String NAVER_TOKEN_URI = "https://nid.naver.com/oauth2.0/token";

	@Override
	public String getGoogleAccessToken(String code) {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("code", code);
		params.add("client_id", googleClientId);
		params.add("client_secret", googleClientSecret);
		params.add("redirect_uri", baseUrl + "/login/oauth2/code/google");
		params.add("grant_type", "authorization_code");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

		GoogleTokenResponse response = restTemplate.postForObject(GOOGLE_TOKEN_URI, request, GoogleTokenResponse.class);
		return response != null ? response.getAccessToken() : null;
	}
}
