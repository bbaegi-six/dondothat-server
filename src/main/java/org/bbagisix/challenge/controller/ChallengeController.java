package org.bbagisix.challenge.controller;

import java.util.List;

import org.bbagisix.challenge.dto.ChallengeDTO;
import org.bbagisix.challenge.dto.ChallengeFailDTO;
import org.bbagisix.challenge.dto.ChallengeProgressDTO;
import org.bbagisix.challenge.service.ChallengeService;
import org.bbagisix.common.exception.BusinessException;
import org.bbagisix.common.exception.ErrorCode;
import org.bbagisix.user.dto.CustomOAuth2User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

	private final ChallengeService challengeService;

	// 1. challengeId가 없는 경우: /api/challenges 또는 /api/challenges/
	@GetMapping
	public ResponseEntity<ChallengeDTO> handleMissingChallengeId() {
		throw new BusinessException(ErrorCode.CHALLENGE_ID_REQUIRED);
	}

	// 2. challengeId가 명시되었지만 "null", 빈값, 숫자 아님 등 예외 처리 포함
	@GetMapping("/{challengeId}")
	public ResponseEntity<ChallengeDTO> getChallengeById(@PathVariable Long challengeId) {
		try {
			ChallengeDTO result = challengeService.getChallengeById(challengeId);
			return ResponseEntity.ok(result);
		} catch (NumberFormatException e) {
			throw new BusinessException(ErrorCode.CHALLENGE_ID_REQUIRED);
		}
	}

	// 2-1. 추천 챌린지 3개 조회 API
	@GetMapping("/recommendations")
	public ResponseEntity<List<ChallengeDTO>> getRecommendedChallenges(Authentication authentication) {
		CustomOAuth2User currentUser = (CustomOAuth2User)authentication.getPrincipal();
		List<ChallengeDTO> recommendedChallenges = challengeService.getRecommendedChallenges(currentUser.getUserId());
		return ResponseEntity.ok(recommendedChallenges);
	}

	// 2-2. 챌린지 진척도 조회 API
	// ChallengeController.java에 추가
	@GetMapping("/progress")
	public ResponseEntity<ChallengeProgressDTO> getChallengeProgress(Authentication authentication) {
		CustomOAuth2User currentUser = (CustomOAuth2User)authentication.getPrincipal();
		ChallengeProgressDTO result = challengeService.getChallengeProgress(currentUser.getUserId());
		return ResponseEntity.ok(result);
	}

	// 3. 챌린지 참여 API (ExpenseController 패턴 적용)
	@PostMapping("/join/{challengeId}/{period}")
	public ResponseEntity<String> joinChallenge(
		@PathVariable Long challengeId,
		@PathVariable Long period,
		Authentication authentication) {

		// challengeId 유효성 검사
		if (challengeId == null) {
			throw new BusinessException(ErrorCode.CHALLENGE_ID_REQUIRED);
		}

		try {
			// ExpenseController와 동일한 패턴으로 사용자 정보 추출
			CustomOAuth2User currentUser = (CustomOAuth2User)authentication.getPrincipal();
			challengeService.joinChallenge(currentUser.getUserId(), challengeId, period);
			return ResponseEntity.ok("챌린지 참여가 완료되었습니다.");

		} catch (NumberFormatException e) {
			throw new BusinessException(ErrorCode.CHALLENGE_ID_REQUIRED);
		}
	}

	// 챌린지 실패 POST /api/challenges/{challenge_id}/fail
	@PostMapping("/{challengeId}/fail")
	public ResponseEntity<List<ChallengeFailDTO>> failChallenge(
		@PathVariable Long challengeId,
		Authentication authentication) {

		// challengeId 유효성 검사
		if (challengeId == null) {
			throw new BusinessException(ErrorCode.CHALLENGE_ID_REQUIRED);
		}

		try {
			CustomOAuth2User currentUser = (CustomOAuth2User)authentication.getPrincipal();
			List<ChallengeFailDTO> failDTOList = challengeService.failChallenge(currentUser.getUserId(), challengeId);
			return ResponseEntity.ok(failDTOList);
		} catch (NumberFormatException e) {
			throw new BusinessException(ErrorCode.CHALLENGE_ID_REQUIRED);
		}
	}

	@PostMapping("/close/{userChallengeId}")
	public ResponseEntity<String> closeChallenge(@PathVariable Long userChallengeId) {
		if (userChallengeId == null) {
			throw new BusinessException(ErrorCode.CHALLENGE_ID_REQUIRED);
		}
		try {
			challengeService.closeChallenge(userChallengeId);
			return ResponseEntity.ok("챌린지 닫기가 완료되었습니다.");
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.CHALLENGE_UPDATE_FAILED);
		}
	}
}
