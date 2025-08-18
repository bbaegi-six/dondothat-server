package org.bbagisix.common.codef.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;

import org.bbagisix.common.codef.domain.CodefAccessTokenVO;
import org.bbagisix.common.codef.mapper.CodefAccessTokenMapper;
import org.bbagisix.common.exception.BusinessException;
import org.bbagisix.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Service
@Log4j2
public class CodefAccessTokenService {

	private static final String OAUTH_URL = "https://oauth.codef.io/oauth/token";

	@Value("${CODEF_CLIENT_ID:}")
	private String clientId;
	@Value("${CODEF_CLIENT_SECRET:}")
	private String clientSecret;

	private final CodefAccessTokenMapper codefAccessTokenMapper;

	private String accessToken;
	private long expiresTime;

	// access token 유효성 확인 및 갱신
	public String getValidAccessToken() {

		CodefAccessTokenVO curToken = codefAccessTokenMapper.getCurrentToken();

		if (curToken == null || isTokenExpired(curToken)) {
			saveAccessToken();  // 토큰 저장
			curToken = codefAccessTokenMapper.getCurrentToken(); // 다시 조회
			if (curToken == null) {
				throw new BusinessException(ErrorCode.CODEF_AUTH_FAIL, "토큰 저장 후에도 토큰을 조회할 수 없습니다.");
			}
		}
		this.accessToken = curToken.getAccessToken();
		this.expiresTime = curToken.getExpiresAt().getTime();

		return this.accessToken;
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
		// API에서 토큰 가져오기
		HashMap<String, Object> tokenMap = getAccessToken();

		if (tokenMap == null) {
			throw new BusinessException(ErrorCode.CODEF_AUTH_FAIL, "Codef OAuth API로부터 토큰을 받지 못했습니다.");
		}

		// 토큰 정보 추출
		Object accessTokenObj = tokenMap.get("access_token");
		Object expiresInObj = tokenMap.get("expires_in");

		if (accessTokenObj == null) {
			throw new BusinessException(ErrorCode.CODEF_AUTH_FAIL, "응답에서 access_token을 찾을 수 없습니다.");
		}

		if (expiresInObj == null) {
			throw new BusinessException(ErrorCode.CODEF_AUTH_FAIL, "응답에서 expires_in을 찾을 수 없습니다.");
		}

		accessToken = tokenMap.get("access_token").toString();

		long expiresIn = Long.parseLong(tokenMap.get("expires_in").toString()) * 1000L;
		expiresTime = System.currentTimeMillis() + expiresIn;

		// VO 생성
		CodefAccessTokenVO vo = new CodefAccessTokenVO();
		vo.setAccessToken(accessToken);
		vo.setExpiresAt(new Date(expiresTime));

		// DB 저장 (기존 토큰이 있으면 업데이트, 없으면 삽입)
		CodefAccessTokenVO curToken = codefAccessTokenMapper.getCurrentToken();

		if (curToken != null) {
			vo.setTokenId(curToken.getTokenId());
			int updatedRows = codefAccessTokenMapper.updateToken(vo);
			if (updatedRows == 0) {
				throw new BusinessException(ErrorCode.CODEF_AUTH_FAIL, "토큰 업데이트에 실패했습니다.");
			}
		} else {
			codefAccessTokenMapper.insertToken(vo);
		}
	}

	// api로 accesstoken 가져옴
	public HashMap<String, Object> getAccessToken() {
		HttpURLConnection con = null;
		BufferedReader br = null;
		try {
			// 클라이언트 ID/Secret 검증
			if (clientId == null || clientId.trim().isEmpty()) {
				throw new BusinessException(ErrorCode.CODEF_AUTH_FAIL, "CODEF_CLIENT_ID가 설정되지 않았습니다.");
			}
			if (clientSecret == null || clientSecret.trim().isEmpty()) {
				throw new BusinessException(ErrorCode.CODEF_AUTH_FAIL, "CODEF_CLIENT_SECRET이 설정되지 않았습니다.");
			}

			URL url = new URL(OAUTH_URL);
			con = (HttpURLConnection)url.openConnection();

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
			if (responseCode == HttpURLConnection.HTTP_OK) {
				br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
			} else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new BusinessException(ErrorCode.CODEF_AUTH_FAIL,
					"Codef OAuth 인증에 실패했습니다. 클라이언트 ID/Secret을 확인해주세요. (HTTP " + responseCode + ")");
			} else {
				// 에러 응답 읽기
				br = new BufferedReader(new InputStreamReader(con.getErrorStream(), StandardCharsets.UTF_8));
				String errorResponse = readResponse(br);
				throw new BusinessException(ErrorCode.CODEF_AUTH_FAIL,
					"Codef OAuth API 요청이 실패했습니다. (HTTP " + responseCode + "): " + errorResponse);
			}
			// 성공 응답 읽기
			String responseStr = readResponse(br);

			// JSON 파싱
			ObjectMapper mapper = new ObjectMapper();
			HashMap<String, Object> result = mapper.readValue(responseStr,
				new TypeReference<HashMap<String, Object>>() {
				});

			log.info("Codef OAuth API에서 토큰 성공적으로 획득");
			return result;

		} catch (IOException e) {
			throw new BusinessException(ErrorCode.CODEF_AUTH_FAIL,
				"Codef OAuth API 네트워크 오류가 발생했습니다: " + e.getMessage());
		} catch (Exception e) {
			// log.error("Access Token 획득 중 예상치 못한 오류", e);
			throw new BusinessException(ErrorCode.CODEF_AUTH_FAIL,
				"Access Token 획득 중 예상치 못한 오류가 발생했습니다: " + e.getMessage());
		} finally {
			// 리소스 정리
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					log.warn("BufferedReader 정리 실패: {}", e.getMessage());
				}
			}
			if (con != null) {
				con.disconnect();
			}
		}
	}

	// 응답 읽기 헬퍼 메서드
	private String readResponse(BufferedReader br) throws IOException {
		StringBuilder responseStr = new StringBuilder();
		String inputLine;
		while ((inputLine = br.readLine()) != null) {
			responseStr.append(inputLine);
		}
		return responseStr.toString();
	}
}
