package org.bbagisix.common.codef.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bbagisix.asset.domain.AssetVO;
import org.bbagisix.asset.dto.AssetDTO;
import org.bbagisix.asset.mapper.AssetMapper;
import org.bbagisix.asset.encryption.EncryptionUtil;
import org.bbagisix.common.codef.dto.CodefTransactionReqDTO;
import org.bbagisix.common.codef.dto.CodefTransactionResDTO;
import org.bbagisix.common.exception.BusinessException;
import org.bbagisix.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Service
@Log4j2
public class CodefApiService {

	@Value("${CODEF_PUBLIC_KEY:}")
	private String publicKey;

	private final CodefAccessTokenService accessTokenService;

	private final EncryptionUtil encryptionUtil;

	private final AssetMapper assetMapper;

	private static final String CONNECTED_ID_URL = "https://development.codef.io/v1/account/create";
	private static final String TRANSACTION_LIST_URL = "https://development.codef.io/v1/kr/bank/p/account/transaction-list";
	private static final String DELETED_URL = "https://development.codef.io/v1/account/delete";

	private final ObjectMapper objectMapper = new ObjectMapper();

	// 은행 코드 매핑
	private static final Map<String, String> BANK_CODES = new HashMap<>();

	static {
		BANK_CODES.put("산업은행", "0002");
		BANK_CODES.put("광주은행", "0034");
		BANK_CODES.put("기업은행", "0003");
		BANK_CODES.put("제주은행", "0035");
		BANK_CODES.put("국민은행", "0004");
		BANK_CODES.put("전북은행", "0037");
		BANK_CODES.put("수협은행", "0007");
		BANK_CODES.put("경남은행", "0039");
		BANK_CODES.put("농협은행", "0011");
		BANK_CODES.put("새마을금고", "0045");
		BANK_CODES.put("우리은행", "0020");
		BANK_CODES.put("신협은행", "0048");
		BANK_CODES.put("SC은행", "0023");
		BANK_CODES.put("우체국", "0071");
		BANK_CODES.put("씨티은행", "0027");
		BANK_CODES.put("하나은행", "0081");
		BANK_CODES.put("대구은행", "0031");
		BANK_CODES.put("신한은행", "0088");
		BANK_CODES.put("부산은행", "0032");
		BANK_CODES.put("K뱅크", "0089");
	}

	// connected id 조회
	public String getConnectedId(AssetDTO assetDTO) {

		// 은행 코드
		String bankCode = BANK_CODES.get(assetDTO.getBankName());
		// 비밀번호 암호화
		String encryptedPw = encryptPw(assetDTO.getBankpw());

		// 요청 본문 생성
		Map<String, Object> reqBody = connectedIdReqBody(bankCode, assetDTO.getBankId(), encryptedPw);

		// API 호출
		Map<String, Object> res = postCodefApi(CONNECTED_ID_URL, reqBody);
		if (res == null) {
			throw new BusinessException(ErrorCode.CODEF_FAIL, "Codef API로부터 응답을 받지 못했습니다.");
		}

		// connected id 추출
		String connectedId = extractConnectedId(res);
		if (connectedId == null) {
			throw new BusinessException(ErrorCode.CODEF_FAIL, "응답에서 Connected ID를 찾을 수 없습니다.");
		}
		return connectedId;
	}

	// Connected ID 요청 본문 생성
	private Map<String, Object> connectedIdReqBody(String bankCode, String bankId, String encryptedPw) {
		Map<String, Object> account = new HashMap<>();
		account.put("countryCode", "KR");
		account.put("businessType", "BK");
		account.put("clientType", "P");
		account.put("organization", bankCode);
		account.put("loginType", "1");
		account.put("id", bankId);
		account.put("password", encryptedPw);

		Map<String, Object> reqBody = new HashMap<>();
		reqBody.put("accountList", new Map[] {account});

		return reqBody;
	}

	// 비밀번호 RSA 암호화
	public String encryptPw(String password) {
		if (password == null || password.trim().isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "암호화할 비밀번호가 비어있습니다.");
		}

		return encryptionUtil.encryptRSA(password, publicKey);
	}

	// 응답에서 Connected ID 추출
	private String extractConnectedId(Map<String, Object> res) {
		Map<String, Object> dataMap = (Map<String, Object>)res.get("data");
		if (dataMap == null || dataMap.get("connectedId") == null) {
			throw new BusinessException(ErrorCode.CODEF_FAIL, "응답 데이터에서 Connected ID를 찾을 수 없습니다.");
		}
		return dataMap.get("connectedId").toString();
	}

	// 거래 내역 조회 요청 DTO 생성
	private CodefTransactionReqDTO createTransactionReqDTO(AssetDTO assetDTO, String connectedId, String startDate,
		String endDate, boolean isFirst) {
		CodefTransactionReqDTO requestDTO = new CodefTransactionReqDTO();

		String bankCode = BANK_CODES.get(assetDTO.getBankName());
		requestDTO.setBankCode(bankCode);

		if (isFirst) {
			String encryptedPassword = encryptPw(assetDTO.getBankpw());
			requestDTO.setBankEncryptPw(encryptedPassword);
		} else {
			requestDTO.setBankEncryptPw(assetDTO.getBankpw());
		}

		requestDTO.setBankId(assetDTO.getBankId());
		requestDTO.setBankAccount(assetDTO.getBankAccount());
		requestDTO.setConnectedId(connectedId);
		requestDTO.setStartDate(startDate);
		requestDTO.setEndDate(endDate);
		requestDTO.setOrderBy("1");

		return requestDTO;
	}

	// 거래 내역 조회
	public CodefTransactionResDTO getTransactionList(AssetDTO assetDTO, String connectedId, String startDate,
		String endDate, boolean isFirst) {

		CodefTransactionReqDTO requestDTO = createTransactionReqDTO(assetDTO, connectedId, startDate, endDate, isFirst);

		Map<String, Object> requestBody = transactionListReqBody(requestDTO);
		Map<String, Object> res = postCodefApi(TRANSACTION_LIST_URL, requestBody);

		if (res == null) {
			throw new BusinessException(ErrorCode.CODEF_FAIL, "거래내역 조회 API로부터 응답을 받지 못했습니다.");
		}

		CodefTransactionResDTO result = toTransactionResDTO(res);
		return result;
	}

	// Connected ID 삭제
	public boolean deleteConnectedId(Long userId) {

		// 사용자 계좌 정보 조회
		AssetVO assetVO = assetMapper.selectAssetByUserIdAndStatus(userId, "main");
		if (assetVO == null) {
			throw new BusinessException(ErrorCode.ASSET_NOT_FOUND);
		}

		String bankName = assetVO.getBankName();
		String connectedId = assetVO.getConnectedId();

		if (connectedId == null || connectedId.trim().isEmpty()) {
			throw new BusinessException(ErrorCode.CODEF_FAIL, "삭제할 Connected ID가 없습니다.");
		}

		String bankCode = BANK_CODES.get(bankName);

		// API 호출
		Map<String, Object> reqBody = deleteConnectedIdReqBody(bankCode, connectedId);
		Map<String, Object> res = postCodefApi(DELETED_URL, reqBody);

		if (res == null) {
			throw new BusinessException(ErrorCode.CODEF_FAIL, "Connected ID 삭제 API로부터 응답을 받지 못했습니다.");
		}

		return true;
	}

	// Connected ID 삭제 요청 본문 생성
	private Map<String, Object> deleteConnectedIdReqBody(String bankCode, String connectedId) {
		Map<String, Object> account = new HashMap<>();
		account.put("countryCode", "KR");
		account.put("businessType", "BK");
		account.put("clientType", "P");
		account.put("organization", bankCode);
		account.put("loginType", "1");

		Map<String, Object> reqBody = new HashMap<>();
		reqBody.put("accountList", new Map[] {account});
		reqBody.put("connectedId", connectedId);
		return reqBody;
	}

	// 거래내역 조회 요청 본문 생성
	private Map<String, Object> transactionListReqBody(CodefTransactionReqDTO requestDTO) {
		Map<String, Object> transaction = new HashMap<>();
		transaction.put("organization", requestDTO.getBankCode());
		transaction.put("connectedId", requestDTO.getConnectedId());
		transaction.put("account", requestDTO.getBankAccount());
		transaction.put("startDate", requestDTO.getStartDate());
		transaction.put("endDate", requestDTO.getEndDate());
		transaction.put("orderBy", requestDTO.getOrderBy());
		transaction.put("inquiryType", "1");
		transaction.put("accountPassword", requestDTO.getBankEncryptPw());
		return transaction;
	}

	// 응답을 CodefTransactionResDTO로 변환
	private CodefTransactionResDTO toTransactionResDTO(Map<String, Object> res) {
		Map<String, Object> dataMap = (Map<String, Object>)res.get("data");
		if (dataMap == null) {
			throw new BusinessException(ErrorCode.CODEF_FAIL, "거래내역 응답 데이터가 없습니다.");
		}

		CodefTransactionResDTO resDTO = new CodefTransactionResDTO();
		resDTO.setResAccountBalance((String)dataMap.get("resAccountBalance"));
		resDTO.setResAccountName((String)dataMap.get("resAccountName"));

		List<Map<String, Object>> historyList = (List<Map<String, Object>>)dataMap.get("resTrHistoryList");
		if (historyList != null) {
			List<CodefTransactionResDTO.HistoryItem> historyItems = historyList.stream()
				.map(this::toHistoryItem)
				.toList();
			resDTO.setResTrHistoryList(historyItems);
		}
		return resDTO;
	}

	// Map을 HistoryItem으로 변환
	private CodefTransactionResDTO.HistoryItem toHistoryItem(Map<String, Object> itemMap) {
		CodefTransactionResDTO.HistoryItem item = new CodefTransactionResDTO.HistoryItem();
		item.setResAccountTrDate((String)itemMap.get("resAccountTrDate"));
		item.setResAccountTrTime((String)itemMap.get("resAccountTrTime"));
		item.setResAccountOut((String)itemMap.get("resAccountOut"));
		item.setResAccountIn((String)itemMap.get("resAccountIn"));
		item.setResAccountDesc1((String)itemMap.get("resAccountDesc1"));
		item.setResAccountDesc2((String)itemMap.get("resAccountDesc2"));
		item.setResAccountDesc3((String)itemMap.get("resAccountDesc3"));
		item.setResAccountDesc4((String)itemMap.get("resAccountDesc4"));
		item.setResAfterTranBalance((String)itemMap.get("resAfterTranBalance"));
		item.setTranDesc((String)itemMap.get("tranDesc"));
		return item;
	}

	// Codef API 공통 호출 메서드
	private Map<String, Object> postCodefApi(String apiURL, Map<String, Object> requestBody) {
		HttpURLConnection con = null;
		BufferedReader br = null;

		try {
			URL url = new URL(apiURL);
			con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json");

			String accessToken = accessTokenService.getValidAccessToken();
			if (accessToken == null) {
				throw new BusinessException(ErrorCode.CODEF_AUTH_FAIL, "유효한 액세스 토큰을 가져올 수 없습니다.");
			}
			con.setRequestProperty("Authorization", "Bearer " + accessToken);

			con.setDoInput(true);
			con.setDoOutput(true);

			// 요청 본문 전송
			String jsonBody = objectMapper.writeValueAsString(requestBody);

			try (OutputStream os = con.getOutputStream()) {
				os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
				os.flush();
			}

			// 응답
			int resCode = con.getResponseCode();
			if (resCode == HttpURLConnection.HTTP_OK) {
				br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
			} else if (resCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new BusinessException(ErrorCode.CODEF_AUTH_FAIL);
			} else {
				br = new BufferedReader(new InputStreamReader(con.getErrorStream(), StandardCharsets.UTF_8));
				String errorResponse = readResponse(br);
				throw new BusinessException(ErrorCode.CODEF_FAIL, "Codef API 요청이 실패했습니다.");
			}

			String resString = readResponse(br);

			// URL 디코딩
			String decodedRes;
			try {
				decodedRes = URLDecoder.decode(resString, StandardCharsets.UTF_8);
			} catch (Exception e) {
				decodedRes = resString;
			}

			return objectMapper.readValue(decodedRes, new TypeReference<Map<String, Object>>() {
			});
		} catch (ProtocolException e) {
			throw new BusinessException(ErrorCode.CODEF_FAIL, "프로토콜 오류가 발생했습니다: " + e.getMessage());
		} catch (MalformedURLException e) {
			throw new BusinessException(ErrorCode.CODEF_FAIL, "잘못된 URL입니다: " + e.getMessage());
		} catch (JsonParseException e) {
			throw new BusinessException(ErrorCode.CODEF_FAIL, "JSON 파싱 오류가 발생했습니다: " + e.getMessage());
		} catch (JsonMappingException e) {
			throw new BusinessException(ErrorCode.CODEF_FAIL, "JSON 매핑 오류가 발생했습니다: " + e.getMessage());
		} catch (JsonProcessingException e) {
			throw new BusinessException(ErrorCode.CODEF_FAIL, "JSON 처리 오류가 발생했습니다: " + e.getMessage());
		} catch (IOException e) {
			throw new BusinessException(ErrorCode.CODEF_FAIL, "네트워크 I/O 오류가 발생했습니다: " + e.getMessage());
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
		StringBuilder resStr = new StringBuilder();
		String inputLine;
		while ((inputLine = br.readLine()) != null) {
			resStr.append(inputLine);
		}
		return resStr.toString();
	}

}