package org.bbagisix.exception;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

	private final String code;
	private final String message;
	private final LocalDateTime timestamp;
	private final String path; // Web API 경로 (null 허용)

	// 일반 HTTP 에러 응답
	public static ErrorResponse of(ErrorCode errorCode) {
		return ErrorResponse.builder()
			.code(errorCode.getCode())
			.message(errorCode.getMessage())
			.timestamp(LocalDateTime.now())
			.build();
	}

	public static ErrorResponse of(ErrorCode errorCode, String path) {
		return ErrorResponse.builder()
			.code(errorCode.getCode())
			.message(errorCode.getMessage())
			.timestamp(LocalDateTime.now())
			.path(path)
			.build();
	}

	// WebSocket 전용 에러 응답
	@Getter
	@Builder
	public static class WebSocketErrorResponse {
		private final String type;             // 고정값: "ERROR"
		private final String code;             // 예: "CH001"
		private final String message;          // 사용자에게 보여줄 메시지
		private final Long challengeId;        // 관련 챌린지 ID
		private final LocalDateTime timestamp;

		public static WebSocketErrorResponse of(ErrorCode errorCode, Long challengeId) {
			return WebSocketErrorResponse.builder()
				.type("ERROR")
				.code(errorCode.getCode())
				.message(errorCode.getMessage())
				.challengeId(challengeId)
				.timestamp(LocalDateTime.now())
				.build();
		}

		public static WebSocketErrorResponse of(ErrorCode errorCode, Long challengeId, String customMessage) {
			return WebSocketErrorResponse.builder()
				.type("ERROR")
				.code(errorCode.getCode())
				.message(customMessage)
				.challengeId(challengeId)
				.timestamp(LocalDateTime.now())
				.build();
		}
	}
}
