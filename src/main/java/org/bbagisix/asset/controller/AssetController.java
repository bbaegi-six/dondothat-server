package org.bbagisix.asset.controller;

import java.util.Map;

import org.bbagisix.asset.dto.AssetDTO;
import org.bbagisix.asset.service.AssetService;
import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
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
		// Service 호출 - 예외는 GlobalExceptionHandler에서 처리
		assetService.connectAsset(userId, assetDTO);

		return ResponseEntity.ok(Map.of(
			"success", true,
			"message", "계좌 연결이 완료되었습니다."
		));
	}

	@DeleteMapping
	public ResponseEntity<Map<String, Object>> deleteAsset(
		@RequestParam Long userId
	) {
		// 입력값 검증
		if (userId == null) {
			throw new BusinessException(ErrorCode.USER_ID_REQUIRED);
		}

		// Service 호출 - 예외는 GlobalExceptionHandler에서 처리
		assetService.deleteAsset(userId);

		return ResponseEntity.ok(Map.of(
			"success", true,
			"message", "계좌가 성공적으로 삭제되었습니다."
		));
	}
}
