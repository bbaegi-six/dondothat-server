package org.bbagisix.challenge.controller;

import lombok.RequiredArgsConstructor;
import org.bbagisix.challenge.dto.ChallengeDTO;
import org.bbagisix.challenge.service.ChallengeService;
import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
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
	public ResponseEntity<ChallengeDTO> getChallengeById(@PathVariable String challengeId) {
		if (challengeId == null || challengeId.trim().isEmpty() || challengeId.equalsIgnoreCase("null")) {
			throw new BusinessException(ErrorCode.CHALLENGE_ID_REQUIRED);
		}

		try {
			Long id = Long.parseLong(challengeId);
			ChallengeDTO result = challengeService.getChallengeById(id);
			return ResponseEntity.ok(result);
		} catch (NumberFormatException e) {
			throw new BusinessException(ErrorCode.CHALLENGE_ID_REQUIRED);
		}
	}
}
