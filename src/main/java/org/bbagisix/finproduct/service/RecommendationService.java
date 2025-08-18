package org.bbagisix.finproduct.service;

import org.bbagisix.common.exception.BusinessException;
import org.bbagisix.common.exception.ErrorCode;
import org.bbagisix.finproduct.dto.LlmSavingProductDTO;
import org.bbagisix.finproduct.dto.LlmSavingRequestDTO;
import org.bbagisix.finproduct.dto.LlmSavingResponseDTO;
import org.bbagisix.finproduct.dto.RecommendedSavingDTO;
import org.bbagisix.finproduct.mapper.FinProductMapper;
import org.bbagisix.user.dto.CustomOAuth2User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

// 적금상품 추천 서비스
// 하이브리드 방식: 백엔드 1차 필터링 + LLM 2차 지능형 추천
@Log4j2
@Service
@RequiredArgsConstructor
public class RecommendationService {

	private final FinProductMapper finProductMapper;
	private final RestTemplate restTemplate = new RestTemplate();

	@Value("${LLM_SERVER_URL}")
	private String llmServerUrl;

	// Authentication에서 사용자 ID 추출하여 추천 서비스 호출
	public List<RecommendedSavingDTO> getRecommendedSavings(Authentication authentication, Integer limit) {
		CustomOAuth2User currentUser = (CustomOAuth2User)authentication.getPrincipal();
		Long userId = currentUser.getUserId();

		log.info("사용자 {}에 대한 적금상품 추천 API 호출 - limit: {}", userId, limit);

		return getFilteredSavings(userId, limit);
	}

	// 사용자 맞춤 적금상품 추천 (하이브리드 방식)
	// 1차: DB 필터링 → 2차: LLM 지능형 추천
	public List<RecommendedSavingDTO> getFilteredSavings(Long userId, Integer limit) {
		try {
			log.info("사용자 {}에 대한 하이브리드 추천 시작 - limit: {}", userId, limit);

			if (userId == null) {
				throw new BusinessException(ErrorCode.USER_ID_REQUIRED, "사용자 ID가 필요합니다");
			}

			// 1차: DB에서 나이 및 직업 필터링 후 조회
			List<RecommendedSavingDTO> filteredProducts = finProductMapper.findRecommendedSavings(userId, null);

			if (filteredProducts == null || filteredProducts.isEmpty()) {
				log.warn("사용자 {}에 대한 추천 가능한 상품이 없습니다", userId);
				throw new BusinessException(ErrorCode.FINPRODUCT_NOT_FOUND, "추천 가능한 금융상품이 없습니다");
			}

			log.info("사용자 {}에 대한 1차 DB 필터링 완료 - {}개 상품", userId, filteredProducts.size());

			// 2차: LLM 서버에서 지능형 추천 (3개)
			List<RecommendedSavingDTO> llmRecommendations = callLlmRecommendation(filteredProducts, userId);

			log.info("사용자 {}에 대한 LLM 추천 완료 - {}개 상품", userId, llmRecommendations.size());

			return llmRecommendations;

		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("하이브리드 추천 중 예상치 못한 오류 발생 - userId: {}, limit: {}, 오류: {}",
				userId, limit, e.getMessage(), e);
			throw new BusinessException(ErrorCode.FINPRODUCT_NOT_FOUND, e);
		}
	}

	// LLM 서버 호출하여 지능형 추천 받기
	private List<RecommendedSavingDTO> callLlmRecommendation(List<RecommendedSavingDTO> filteredProducts, Long userId) {
		try {
			// 사용자 정보 추출 (첫 번째 상품에서)
			RecommendedSavingDTO firstProduct = filteredProducts.get(0);
			int userAge = firstProduct.getUserAge();
			String userJob = firstProduct.getUserJob();
			String mainBankName = firstProduct.getMainBankName();

			// LLM 요청 데이터 구성
			List<LlmSavingProductDTO> llmProducts = filteredProducts.stream()
				.map(product -> LlmSavingProductDTO.builder()
					.finPrdtCd(product.getFinPrdtCd())
					.korCoNm(product.getKorCoNm())
					.finPrdtNm(product.getFinPrdtNm())
					.spclCnd(product.getSpclCnd())
					.joinMember(product.getJoinMember())
					.intrRate(product.getIntrRate().doubleValue())
					.intrRate2(product.getIntrRate2().doubleValue())
					.build())
				.collect(Collectors.toList());

			LlmSavingRequestDTO request = LlmSavingRequestDTO.builder()
				.savings(llmProducts)
				.userAge(userAge)
				.userJob(userJob)
				.mainBankName(mainBankName)
				.build();

			// HTTP 요청 헤더 설정
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<LlmSavingRequestDTO> entity = new HttpEntity<>(request, headers);
			// LLM 서버 호출
			String llmEndpoint = llmServerUrl + "/recommend-savings";
			log.info("LLM 서버 호출: {} - 사용자: {}, 상품수: {}", llmEndpoint, userId, llmProducts.size());

			ResponseEntity<LlmSavingResponseDTO> response = restTemplate.exchange(
				llmEndpoint,
				HttpMethod.POST,
				entity,
				LlmSavingResponseDTO.class
			);

			if (response.getBody() != null && response.getBody().getRecommendations() != null) {
				// LLM 추천 결과 반환 (상품 정보만)
				return response.getBody().getRecommendations().stream()
					.map(product -> RecommendedSavingDTO.builder()
						.finPrdtCd(product.getFinPrdtCd())
						.korCoNm(product.getKorCoNm())
						.finPrdtNm(product.getFinPrdtNm())
						.spclCnd(product.getSpclCnd())
						.joinMember(product.getJoinMember())
						.intrRate(BigDecimal.valueOf(product.getIntrRate()))
						.intrRate2(BigDecimal.valueOf(product.getIntrRate2()))
						.build())
					.collect(Collectors.toList());
			} else {
				log.warn("LLM 서버에서 빈 응답 - 사용자: {}", userId);
				// LLM 실패 시 상위 3개 상품 반환 (상품 정보만)
				return filteredProducts.stream()
					.limit(3)
					.map(product -> RecommendedSavingDTO.builder()
						.finPrdtCd(product.getFinPrdtCd())
						.korCoNm(product.getKorCoNm())
						.finPrdtNm(product.getFinPrdtNm())
						.spclCnd(product.getSpclCnd())
						.joinMember(product.getJoinMember())
						.intrRate(product.getIntrRate())
						.intrRate2(product.getIntrRate2())
						.build())
					.collect(Collectors.toList());
			}

		} catch (Exception e) {
			log.error("LLM 서버 호출 실패 - 사용자: {}, 오류: {}", userId, e.getMessage(), e);
			// LLM 실패 시 상위 3개 상품 반환 (상품 정보만)
			return filteredProducts.stream()
				.limit(3)
				.map(product -> RecommendedSavingDTO.builder()
					.finPrdtCd(product.getFinPrdtCd())
					.korCoNm(product.getKorCoNm())
					.finPrdtNm(product.getFinPrdtNm())
					.spclCnd(product.getSpclCnd())
					.joinMember(product.getJoinMember())
					.intrRate(product.getIntrRate())
					.intrRate2(product.getIntrRate2())
					.build())
				.collect(Collectors.toList());
		}
	}
}