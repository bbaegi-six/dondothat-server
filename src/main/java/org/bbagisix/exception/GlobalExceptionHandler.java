package org.bbagisix.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final SimpMessagingTemplate messagingTemplate;

	// ✅ WebSocket용 BusinessException 처리
	@MessageExceptionHandler(BusinessException.class)
	public void handleWsBusinessException(BusinessException e) {
		log.warn("WebSocket BusinessException 발생: code={}, message={}", e.getCode(), e.getMessage());

		ErrorResponse.WebSocketErrorResponse errorResponse =
			ErrorResponse.WebSocketErrorResponse.of(e.getErrorCode(), extractChallengeId(e));

		Long challengeId = extractChallengeId(e);
		if (challengeId != null) {
			messagingTemplate.convertAndSend("/topic/chat/" + challengeId, errorResponse);
		}
	}

	// ✅ WebSocket용 Validation 예외 처리
	@MessageExceptionHandler(ConstraintViolationException.class)
	public void handleWsValidationException(ConstraintViolationException e) {
		log.warn("WebSocket ConstraintViolationException 발생: {}", e.getMessage());

		ErrorResponse.WebSocketErrorResponse errorResponse =
			ErrorResponse.WebSocketErrorResponse.of(ErrorCode.INVALID_REQUEST, null);
		// 전체 broadcast는 생략 (또는 필요 시 채널 구성)
	}

	// ✅ WebSocket용 일반 Runtime 예외 처리
	@MessageExceptionHandler(RuntimeException.class)
	public void handleWsRuntimeException(RuntimeException e) {
		log.error("WebSocket RuntimeException 발생: {}", e.getMessage(), e);

		ErrorResponse.WebSocketErrorResponse errorResponse =
			ErrorResponse.WebSocketErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, null);
		// broadcast 생략
	}

	// ✅ REST API용 BusinessException 처리
	@ExceptionHandler(BusinessException.class)
	@ResponseBody
	public ResponseEntity<ErrorResponse> handleRestBusinessException(BusinessException e, HttpServletRequest request) {
		log.warn("REST BusinessException 발생: code={}, message={}", e.getCode(), e.getMessage());

		return ResponseEntity
			.status(e.getHttpStatus())
			.body(ErrorResponse.of(e.getErrorCode(), request.getRequestURI()));
	}

	// ✅ REST API용 Validation 예외 처리
	@ExceptionHandler(ConstraintViolationException.class)
	@ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
	@ResponseBody
	public ErrorResponse handleRestValidationException(ConstraintViolationException e, HttpServletRequest request) {
		log.warn("REST ConstraintViolationException 발생: {}", e.getMessage());

		return ErrorResponse.of(ErrorCode.INVALID_REQUEST, request.getRequestURI());
	}

	// ✅ REST API용 일반 예외 처리
	@ExceptionHandler(Exception.class)
	@ResponseStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody
	public ErrorResponse handleRestGeneralException(Exception e, HttpServletRequest request) {
		log.error("REST General Exception 발생: {}", e.getMessage(), e);

		return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI());
	}

	// ✅ 추후 확장을 위한 challengeId 추출 로직
	private Long extractChallengeId(BusinessException e) {
		// TODO: BusinessException에 challengeId 포함 시 확장 가능
		return null;
	}
}
