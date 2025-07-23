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

	@GetMapping("/{challengeId}")
	public ResponseEntity<ChallengeDTO> getChallengeById(@PathVariable Long challengeId) {
		ChallengeDTO result = challengeService.getChallengeById(challengeId);
		return ResponseEntity.ok(result);
	}
}
