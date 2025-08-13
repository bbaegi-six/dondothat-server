package org.bbagisix.finproduct.controller;

import org.bbagisix.finproduct.dto.RecommendedSavingDTO;
import org.bbagisix.finproduct.service.FinProductService;
import org.bbagisix.finproduct.service.RecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;

// 금융상품 API 컨트롤러
// 금감원 데이터 동기화 및 맞춤형 적금상품 추천 기능 제공
@RestController
@RequestMapping("/api/finproduct")
@RequiredArgsConstructor
@Log4j2
public class FinProductController {

	private final FinProductService finProductService;
	private final RecommendationService recommendationService;


	// 금감원 적금상품 데이터 동기화
	// 스케줄러에서 호출하여 최신 데이터로 업데이트
	@PostMapping("/sync")
	public ResponseEntity<String> syncFinProductData() {
		log.info("금융상품 데이터 동기화 API 호출");

		finProductService.syncFinProductData();

		log.info("금융상품 데이터 동기화 API 완료");
		return new ResponseEntity<>("금융상품 데이터 동기화가 완료되었습니다.", HttpStatus.OK);
	}

	// 사용자 맞춤 적금상품 추천 조회
	// 하이브리드 방식: 백엔드 1차 필터링 + LLM 2차 지능형 추천
	@GetMapping("/recommend")
	public ResponseEntity<List<RecommendedSavingDTO>> getRecommendedSavings(
		Authentication authentication,
		@RequestParam(value = "limit", required = false) Integer limit) {

		List<RecommendedSavingDTO> recommendations = recommendationService.getRecommendedSavings(authentication, limit);

		return ResponseEntity.ok(recommendations);
	}
}