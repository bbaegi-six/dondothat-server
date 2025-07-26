package org.bbagisix.asset.controller;

import java.util.Map;

import org.bbagisix.asset.dto.AssetDTO;
import org.bbagisix.asset.service.AssetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Log4j2
public class AssetController {

	private final AssetService assetService;

	@PostMapping("/connect")
	public ResponseEntity<Map<String, Object>> connectAsset(
		@RequestBody AssetDTO assetDTO,
		@RequestParam Long userId
		) {
		boolean success = assetService.connectAsset(userId, assetDTO);

		if (success) {
			return ResponseEntity.ok(Map.of(
				"success", true,
				"message", "계좌 연결 완료"
			));
		} else {
			return ResponseEntity.badRequest().body(Map.of(
				"success", false,
				"message", "계좌 연결 실패"
			));
		}
	}

	@DeleteMapping
	public ResponseEntity<Map<String, Object>> deleteAsset(
		@RequestParam Long userId
	) {
		boolean success = assetService.deleteAsset(userId);

		if (success) {
			return ResponseEntity.ok(Map.of(
				"success", true,
				"message", "계좌 삭제 완료"
			));
		} else {
			return ResponseEntity.badRequest().body(Map.of(
				"success", false,
				"message", "계좌 삭제 실패"
			));
		}
	}

}
