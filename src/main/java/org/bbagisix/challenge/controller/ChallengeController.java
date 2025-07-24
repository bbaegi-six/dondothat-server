package org.bbagisix.challenge.controller;

import lombok.RequiredArgsConstructor;
import org.bbagisix.challenge.dto.ChallengeDTO;
import org.bbagisix.challenge.service.ChallengeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

	private final ChallengeService challengeService;

	// ✅ 단일 챌린지 상세 조회 (User ID 없이 테스트용)
	@GetMapping("/{challengeId}")
	public ResponseEntity<ChallengeDTO> getChallengeById(@PathVariable Long challengeId) {
		ChallengeDTO result = challengeService.getChallengeById(challengeId);
		return ResponseEntity.ok(result);
	}
}
