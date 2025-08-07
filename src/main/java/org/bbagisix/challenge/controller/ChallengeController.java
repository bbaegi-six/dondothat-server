package org.bbagisix.challenge.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.bbagisix.challenge.dto.ChallengeDTO;
import org.bbagisix.challenge.service.ChallengeService;
import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
import org.bbagisix.user.dto.CustomOAuth2User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
		CustomOAuth2User currentUser = (CustomOAuth2User) authentication.getPrincipal();
		List<ChallengeDTO> recommendedChallenges = challengeService.getRecommendedChallenges(currentUser.getUserId());
		return ResponseEntity.ok(recommendedChallenges);
	}

	// 3. 챌린지 참여 API (ExpenseController 패턴 적용)
	@PostMapping("/{challengeId}/join")
	public ResponseEntity<String> joinChallenge(
		@PathVariable String challengeId,
		Authentication authentication) {

		// challengeId 유효성 검사
		if (challengeId == null || challengeId.trim().isEmpty() || challengeId.equalsIgnoreCase("null")) {
			throw new BusinessException(ErrorCode.CHALLENGE_ID_REQUIRED);
		}

		try {
			Long parsedChallengeId = Long.parseLong(challengeId);

			// ExpenseController와 동일한 패턴으로 사용자 정보 추출
			CustomOAuth2User currentUser = (CustomOAuth2User) authentication.getPrincipal();

			challengeService.joinChallenge(parsedChallengeId, currentUser.getUserId());
			return ResponseEntity.ok("챌린지 참여가 완료되었습니다.");

		} catch (NumberFormatException e) {
			throw new BusinessException(ErrorCode.CHALLENGE_ID_REQUIRED);
		}
	}

	// 4. 챌린지 탈퇴 API (ExpenseController 패턴 적용)
	@DeleteMapping("/{challengeId}/leave")
	public ResponseEntity<String> leaveChallenge(
		@PathVariable String challengeId,
		Authentication authentication) {

		// challengeId 유효성 검사
		if (challengeId == null || challengeId.trim().isEmpty() || challengeId.equalsIgnoreCase("null")) {
			throw new BusinessException(ErrorCode.CHALLENGE_ID_REQUIRED);
		}

		try {
			Long parsedChallengeId = Long.parseLong(challengeId);

			// ExpenseController와 동일한 패턴으로 사용자 정보 추출
			CustomOAuth2User currentUser = (CustomOAuth2User) authentication.getPrincipal();

			challengeService.leaveChallenge(parsedChallengeId, currentUser.getUserId());
			return ResponseEntity.ok("챌린지 탈퇴가 완료되었습니다.");

		} catch (NumberFormatException e) {
			throw new BusinessException(ErrorCode.CHALLENGE_ID_REQUIRED);
		}
	}
}