package org.bbagisix.chat.exception;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final SimpMessagingTemplate messagingTemplate;

	/**
	 * Business Exception 처리 (WebSocket용)
	 */
	@MessageExceptionHandler(BusinessException.class)
	public void handleBusinessException(BusinessException e) {
		log.warn("Business Exception 발생: code={}, message={}", e.getCode(), e.getMessage());

		// WebSocket으로 에러 메시지 전송
		ErrorResponse.WebSocketErrorResponse errorResponse =
			ErrorResponse.WebSocketErrorResponse.of(e.getErrorCode(), extractChallengeId(e));

		// 해당 챌린지 채팅방으로 에러 메시지 전송
		Long challengeId = extractChallengeId(e);
		if (challengeId != null) {
			messagingTemplate.convertAndSend("/topic/chat/" + challengeId, errorResponse);
		}
	}

	/**
	 * Validation Exception 처리 (WebSocket용)
	 */
	@MessageExceptionHandler(ConstraintViolationException.class)
	public void handleValidationException(ConstraintViolationException e) {
		log.warn("Validation Exception 발생: {}", e.getMessage());

		ErrorResponse.WebSocketErrorResponse errorResponse =
			ErrorResponse.WebSocketErrorResponse.of(ErrorCode.INVALID_REQUEST, null);

		// 전체 에러이므로 특정 챌린지로 전송하지 않음
	}

	/**
	 * 일반적인 RuntimeException 처리 (WebSocket용)
	 */
	@MessageExceptionHandler(RuntimeException.class)
	public void handleRuntimeException(RuntimeException e) {
		log.error("예상하지 못한 Runtime Exception 발생: {}", e.getMessage(), e);

		ErrorResponse.WebSocketErrorResponse errorResponse =
			ErrorResponse.WebSocketErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, null);

		// 시스템 전체 오류이므로 특정 챌린지로 전송하지 않음
	}

	/**
	 * Business Exception 처리 (REST API용)
	 */
	@ExceptionHandler(BusinessException.class)
	@ResponseBody
	public ErrorResponse handleBusinessExceptionForRest(BusinessException e, HttpServletRequest request) {
		log.warn("REST Business Exception 발생: code={}, message={}", e.getCode(), e.getMessage());

		return ErrorResponse.of(e.getErrorCode(), request.getRequestURI());
	}

	/**
	 * Validation Exception 처리 (REST API용)
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	@ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
	@ResponseBody
	public ErrorResponse handleValidationExceptionForRest(ConstraintViolationException e, HttpServletRequest request) {
		log.warn("REST Validation Exception 발생: {}", e.getMessage());

		return ErrorResponse.of(ErrorCode.INVALID_REQUEST, request.getRequestURI());
	}

	/**
	 * 일반적인 Exception 처리 (REST API용)
	 */
	@ExceptionHandler(Exception.class)
	@ResponseStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody
	public ErrorResponse handleGeneralExceptionForRest(Exception e, HttpServletRequest request) {
		log.error("REST General Exception 발생: {}", e.getMessage(), e);

		return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI());
	}

	/**
	 * Exception에서 challengeId 추출
	 * TODO: 추후 BusinessException에 challengeId를 포함하도록 개선
	 */
	private Long extractChallengeId(BusinessException e) {
		// 현재는 null 반환
		// 추후 Exception 생성 시 challengeId를 포함하도록 구조 개선 필요
		return null;
	}
}
