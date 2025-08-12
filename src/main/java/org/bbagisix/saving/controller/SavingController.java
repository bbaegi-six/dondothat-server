package org.bbagisix.saving.controller;

import java.util.List;

import org.bbagisix.saving.dto.SavingDTO;
import org.bbagisix.saving.service.SavingService;
import org.bbagisix.user.dto.CustomOAuth2User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/api/saving")
@RequiredArgsConstructor
@Log4j2
public class SavingController {
	private final SavingService savingService;

	@GetMapping("/total")
	public ResponseEntity<Long> getTotalSaving(Authentication authentication) {
		CustomOAuth2User currentUser = (CustomOAuth2User)authentication.getPrincipal();
		Long total = savingService.getTotalSaving(currentUser.getUserId());
		return ResponseEntity.ok(total);
	}

	@GetMapping("/history")
	public ResponseEntity<List<SavingDTO>> getSavingHistory(Authentication authentication) {
		CustomOAuth2User currentUser = (CustomOAuth2User)authentication.getPrincipal();
		List<SavingDTO> savingHistory = savingService.getSavingHistory(currentUser.getUserId());
		return ResponseEntity.ok(savingHistory);
	}

	@PostMapping("/save/{userChallengeId}")
	public ResponseEntity<String> saveSaving(@PathVariable Long userChallengeId, Authentication authentication) {
		CustomOAuth2User currentUser = (CustomOAuth2User)authentication.getPrincipal();
		Long userId = currentUser.getUserId();
		savingService.updateSaving(userId, userChallengeId);
		return ResponseEntity.ok("저금통 저장 완료");
	}
}
