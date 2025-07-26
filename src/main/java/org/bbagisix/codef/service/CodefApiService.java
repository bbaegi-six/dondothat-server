package org.bbagisix.codef.service;

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
import org.bbagisix.codef.EncryptionUtil;
import org.bbagisix.codef.dto.CodefTransactionReqDTO;
import org.bbagisix.codef.dto.CodefTransactionResDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CodefApiService {

	@Value("${codef.publicKey}")
	private String publicKey;

	@Autowired(required = false)
	private CodefAccessTokenService accessTokenService;

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

	@Autowired
	private AssetMapper assetMapper;

	// connected id 조회
	public String getConnectedId(AssetDTO assetDTO) {
		try {
			String bankCode = BANK_CODES.get(assetDTO.getBankName());
			String encryptedPw = encryptPw(assetDTO.getBankpw());

			Map<String, Object> reqBody = connectedIdReqBody(bankCode, assetDTO.getBankId(), encryptedPw);
			Map<String, Object> res = postCodefApi(CONNECTED_ID_URL, reqBody);

			if(res == null){
				// 💥 에러 메시지 : 들어온 메시지가 없음
			}
			return extractConnectedId(res);

		} catch (Exception e) {
			// 💥 connectedid 조회 실패
			return null;
		}
	}

	private Map<String, Object> connectedIdReqBody(String bankCode,String bankId, String encryptedPw){
		Map<String, Object> account = new HashMap<>();
		account.put("countryCode", "KR");
		account.put("businessType", "BK");
		account.put("clientType", "P");
		account.put("organization", bankCode);
		account.put("loginType", "1");
		account.put("id", bankId);
		account.put("password", encryptedPw);


		Map<String, Object> reqBody = new HashMap<>();
		reqBody.put("accountList", new Map[]{account});

		return reqBody;
	}

	private String encryptPw(String password) {
		EncryptionUtil encryptionUtil = new EncryptionUtil();

		return encryptionUtil.encryptRSA(password, publicKey);
	}

	private String extractConnectedId(Map<String, Object> res) {
		try{
			Map<String,Object> dataMap = (Map<String, Object>) res.get("data");
			if(dataMap == null || dataMap.get("connectedId") == null){
				// 💥 에러 메시지 connectedid 찾을 수 없습니다
				return null;
			}
			return dataMap.get("connectedId").toString();
		} catch (Exception e) {
			// 💥 에러 메시지
			return null;
		}

	}



	private CodefTransactionReqDTO createTransactionReqDTO(AssetDTO assetDTO, String connectedId, String startDate, String endDate) {
		CodefTransactionReqDTO requestDTO = new CodefTransactionReqDTO();

		String bankCode = BANK_CODES.get(assetDTO.getBankName());
		requestDTO.setBankCode(bankCode);

		String encryptedPassword = encryptPw(assetDTO.getBankpw());
		requestDTO.setBankEncryptPw(encryptedPassword);

		requestDTO.setBankId(assetDTO.getBankId());
		requestDTO.setBankAccount(assetDTO.getBankAccount());
		requestDTO.setConnectedId(connectedId);
		requestDTO.setStartDate(startDate);
		requestDTO.setEndDate(endDate);
		requestDTO.setOrderBy("0");

		return requestDTO;
	}



	// 거래 내역 조회
	public CodefTransactionResDTO getTransactionList(AssetDTO assetDTO, String connectedId, String startDate, String endDate) {
		try {
			CodefTransactionReqDTO requestDTO = createTransactionReqDTO(assetDTO, connectedId, startDate, endDate);

			Map<String, Object> requestBody = transactionListReqBody(requestDTO);
			Map<String, Object> res = postCodefApi(TRANSACTION_LIST_URL, requestBody);

			if(res == null){
				// 💥 에러메시지 : 거래내역 api 응답이 null 입니다
				return null;
			}

			return toTransactionResDTO(res);
		} catch (Exception e) {
			// 💥 throw new RuntimeException("거래내역 조회 실패: " + e.getMessage(), e);
			return null;
		}
	}


	public boolean deleteConnectedId(Long userId) {
		try {
			// 은행 값과 connectedid 를 user_asset에서 userId 가 같은 것으로 가져옴
			AssetVO assetVO = assetMapper.selectAssetByUserId(userId);

			String bankName = assetVO.getBankName();
			String connectedId = assetVO.getConnectedId();

			String bankCode = BANK_CODES.get(bankName);

			Map<String, Object> reqBody = deleteConnectedIdReqBody(bankCode, connectedId);
			Map<String, Object> res = postCodefApi(DELETED_URL, reqBody);

			if(res == null){
				// 💥 에러 메시지 : 들어온 메시지가 없음
				return false;
			}

			return true;

		} catch (Exception e) {
			// 💥 connectedid 조회 실패
			return false;
		}
	}

	private Map<String, Object> deleteConnectedIdReqBody(String bankCode, String connectedId) {
		Map<String, Object> account = new HashMap<>();
		account.put("countryCode", "KR");
		account.put("businessType", "BK");
		account.put("clientType", "P");
		account.put("organization", bankCode);
		account.put("loginType", "1");

		Map<String, Object> reqBody = new HashMap<>();
		reqBody.put("accountList", new Map[]{account});
		reqBody.put("connectedId",connectedId);
		return reqBody;
	}

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

	private CodefTransactionResDTO toTransactionResDTO(Map<String, Object> res){
		try{
			Map<String, Object> dataMap = (Map<String, Object>) res.get("data");
			if(dataMap == null){
				// 💥 에러 메시지 : 거래내역 응답 데이터가 없습니다.
			}

			CodefTransactionResDTO resDTO = new CodefTransactionResDTO();
			resDTO.setResAccountBalance((String) dataMap.get("resAccountBalance"));
			resDTO.setResAccountName((String) dataMap.get("resAccountName"));

			List<Map<String, Object>> historyList = (List<Map<String, Object>>) dataMap.get("resTrHistoryList");
			if(historyList != null){
				List<CodefTransactionResDTO.HistoryItem> historyItems = historyList.stream()
					.map(this::toHistoryItem)
					.toList();
				resDTO.setResTrHistoryList(historyItems);
			}
			return resDTO;
		} catch (Exception e){
			// 💥 에러메시지 : 거래내역 응답 데이터 변환 실패
			return null;
		}
	}
	private CodefTransactionResDTO.HistoryItem toHistoryItem(Map<String, Object> itemMap){
		CodefTransactionResDTO.HistoryItem item = new CodefTransactionResDTO.HistoryItem();
		item.setResAccountTrDate((String) itemMap.get("resAccountTrDate"));
		item.setResAccountTrTime((String) itemMap.get("resAccountTrTime"));
		item.setResAccountOut((String) itemMap.get("resAccountOut"));
		item.setResAccountIn((String) itemMap.get("resAccountIn"));
		item.setResAccountDesc1((String) itemMap.get("resAccountDesc1"));
		item.setResAccountDesc2((String) itemMap.get("resAccountDesc2"));
		item.setResAccountDesc3((String) itemMap.get("resAccountDesc3"));
		item.setResAccountDesc4((String) itemMap.get("resAccountDesc4"));
		item.setResAfterTranBalance((String) itemMap.get("resAfterTranBalance"));
		item.setTranDesc((String) itemMap.get("tranDesc"));
		return item;
	}

	private Map<String, Object> postCodefApi(String apiURL, Map<String, Object> requestBody) {
		HttpURLConnection con = null;
		BufferedReader br = null;

		try {
			URL url = new URL(apiURL);
			con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json");

			String accessToken = accessTokenService.getValidAccessToken();
			con.setRequestProperty("Authorization", "Bearer " + accessToken);

			con.setDoInput(true);
			con.setDoOutput(true);

			String jsonBody = objectMapper.writeValueAsString(requestBody);

			try (OutputStream os = con.getOutputStream()) {
				os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
				os.flush();
			}

			// 응답
			int resCode = con.getResponseCode();
			if (resCode == HttpURLConnection.HTTP_OK) {
				br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
			} else {
				br = new BufferedReader(new InputStreamReader(con.getErrorStream(), StandardCharsets.UTF_8));
				// 💥 에러 메시지 : 요청error
			}

			String inputLine;
			StringBuilder resStr = new StringBuilder();
			while ((inputLine = br.readLine()) != null) {
				resStr.append(inputLine);
			}

			String resString = resStr.toString();

			String decodedRes; // 응답 디코딩
			try {
				decodedRes = URLDecoder.decode(resString, StandardCharsets.UTF_8);
				// System.out.println("디코딩된 응답: " + decodedRes); // 디버깅용
			} catch (Exception e) {
				// 💥 에러 메시지
				// URL 디코딩 실패시 원본 사용
				decodedRes = resString;
			}

			return objectMapper.readValue(decodedRes, new TypeReference<Map<String, Object>>() {});

		} catch (ProtocolException e) {
			// 💥 에러 메시지 : 프로토콜 오류
		} catch (MalformedURLException e) {
			// 💥 에러 메시지 : 잘못된 URL
		} catch (IOException e) {
			// 💥 에러 메시지 : I/O 오류
		} catch (Exception e) {
			// 💥 에러 메시지 : API 호출 실패
		} finally {
			// 리소스 정리
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					// 💥 에러 메시지 : BufferedReader 정리 실패
				}
			}
			if (con != null) {
				con.disconnect();
			}
		}
		return null;
	}


}