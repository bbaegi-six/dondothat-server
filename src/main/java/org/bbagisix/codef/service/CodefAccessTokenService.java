package org.bbagisix.codef.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.sql.Timestamp;

import org.bbagisix.codef.domain.CodefAccessTokenVO;
import org.bbagisix.codef.mapper.CodefAccessTokenMapper;
import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;



@Service
public class CodefAccessTokenService {

	private static final String OAUTH_URL = "https://oauth.codef.io/oauth/token";

	@Value("${codef.clientId}")
	private String clientId;
	@Value("${codef.clientSecret}")
	private String clientSecret;

	@Autowired
	private CodefAccessTokenMapper codefAccessTokenMapper;

	private String accessToken;
	private long expiresTime;

	// access token 유효성 확인 및 갱신
	public String getValidAccessToken(){
		try {
			CodefAccessTokenVO curToken = codefAccessTokenMapper.getCurrentToken();

			if (curToken == null || isTokenExpired(curToken)) {
				saveAccessToken();  // 토큰 저장
				curToken = codefAccessTokenMapper.getCurrentToken(); // 다시 조회
			}

			this.accessToken = curToken.getAccessToken();
			this.expiresTime = curToken.getExpiresAt().getTime();

			return this.accessToken;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessException(ErrorCode.CODEF_ACCCESS_TOKEN_FAILED);
		}
	}

	// 토큰 만료 여부 확인
	private boolean isTokenExpired(CodefAccessTokenVO token) {
		if (token == null || token.getExpiresAt() == null) {
			return true;
		}

		long currentTime = System.currentTimeMillis();
		long tokenExpiryTime = token.getExpiresAt().getTime();

		// 만료 10분 전에 미리 갱신
		long bufferTime = 10 * 60 * 1000L; // 10분

		return (currentTime + bufferTime) >= tokenExpiryTime;
	}


	// 가져온 token을 token table에 저장
	public void saveAccessToken() {
		try {
			HashMap<String, Object> tokenMap = getAccessToken();

			System.out.println(tokenMap);
			if (tokenMap == null) {
				throw new BusinessException(ErrorCode.CODEF_AUTHENTICATION_FAILED,
					"Codef API로부터 토큰을 받지 못했습니다.");
			}

			accessToken = tokenMap.get("access_token").toString();

			long expiresIn = Long.parseLong(tokenMap.get("expires_in").toString()) * 1000L;
			expiresTime = System.currentTimeMillis() + expiresIn;

			CodefAccessTokenVO vo = new CodefAccessTokenVO();
			vo.setAccessToken(accessToken);
			vo.setExpiresAt(new Timestamp(expiresTime));

			CodefAccessTokenVO curToken = codefAccessTokenMapper.getCurrentToken();

			if (curToken != null) {
				vo.setTokenId(curToken.getTokenId());
				codefAccessTokenMapper.updateToken(vo);
			} else {
				codefAccessTokenMapper.insertToken(vo);
			}
		} catch (Exception err) {
			// err.printStackTrace();
		}
	}

	// api로 accesstoken 가져옴
	public HashMap<String, Object> getAccessToken() {
		HttpURLConnection con = null;
		BufferedReader br = null;
		try {
			URL url = new URL(OAUTH_URL);
			con = (HttpURLConnection) url.openConnection();

			// body
			String params = "grant_type=client_credentials&scope=read";

			// header
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			// auth : 클라이언트아이디, 시크릿코드 Base64 인코딩
			String auth = clientId + ":" + clientSecret;
			String authStringEnc = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

			con.setRequestProperty("Authorization", "Basic " + authStringEnc);
			con.setDoInput(true);
			con.setDoOutput(true);

			// 요청
			try (OutputStream os = con.getOutputStream()) {
				os.write(params.getBytes());
				os.flush();
			}

			// 응답
			int responseCode = con.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {    // 정상 응답
				br = new BufferedReader(new InputStreamReader(con.getInputStream()));
			} else {     // 에러 발생
				return null;
			}

			String inputLine;
			StringBuffer responseStr = new StringBuffer();
			while ((inputLine = br.readLine()) != null) {
				responseStr.append(inputLine);
			}
			br.close();

			// 응답결과 URL Decoding(UTF-8)
			ObjectMapper mapper = new ObjectMapper();
			System.out.println(mapper);
			return mapper.readValue(responseStr.toString(), new TypeReference<HashMap<String, Object>>() {
			});

		} catch (Exception e) {
			//e.printStackTrace();
			return null;
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
				}
			}
		}
	}
}
