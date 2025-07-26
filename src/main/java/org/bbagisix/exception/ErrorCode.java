package org.bbagisix.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// 메시지 관련 에러
	INVALID_MESSAGE(HttpStatus.BAD_REQUEST, "C001", "메시지가 유효하지 않습니다."),
	MESSAGE_TOO_LONG(HttpStatus.BAD_REQUEST, "C002", "메시지가 너무 깁니다. (최대 255자)"),
	MESSAGE_EMPTY(HttpStatus.BAD_REQUEST, "C003", "메시지를 입력해주세요"),
	MESSAGE_CONTAINS_HTML(HttpStatus.BAD_REQUEST, "C004", "메시지에 HTML 태그를 포함할 수 없습니다."),

	// 사용자 관련 에러
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
	USER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "U002", "사용자 ID는 필수입니다."),
	USER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "U003", "권한이 없습니다."),

	// 회원가입
	EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "E002", "이미 사용 중인 이메일입니다."),
	INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "E003", "인증코드가 올바르지 않습니다."),
	VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "E004", "인증코드가 만료되었습니다."),
	EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E001", "이메일 발송에 실패했습니다."),

	// 챌린지 관련 에러
	CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CH001", "챌린지를 찾을 수 없습니다."),
	CHALLENGE_ID_REQUIRED(HttpStatus.BAD_REQUEST, "CH002", "챌린지 ID는 필수입니다."),
	CHALLENGE_ENDED(HttpStatus.BAD_REQUEST, "CH003", "종료된 챌린지입니다."),
	CHALLENGE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CH004", "해당 챌린지에 참여할 권한이 없습니다."),

	// 데이터베이스 관련 에러
	DATA_ACCESS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "D001", "일시적인 오류가 발생했습니다."),
	MESSAGE_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "D002", "메시지 저장에 실패했습니다."),
	MESSAGE_LOAD_FAILED(HttpStatus.INSUFFICIENT_STORAGE, "D003", "메시지를 불러올 수 없습니다."),

	// WebSocket 관련 에러
	WEBSOCKET_CONNECTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "W001", "채팅 연결에 실패했습니다."),
	WEBSOCKET_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "W002", "메시지 전송에 실패했습니다."),
	SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "W003", "세션이 만료되었습니다. 다시 연결해주세요."),

	// LLM 관련 에러
	LLM_CLASSIFY_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "L001", "카테고리 분류에 실패했습니다."),
	LLM_ANALYTICS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "L002", "과소비 카테고리 분석에 실패했습니다."),

	// 일반적인 시스템 에러
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "서버 내부 오류가 발생했습니다."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "S002", "잘못된 요청입니다."),
	SERVICE_UNVALIABLE(HttpStatus.SERVICE_UNAVAILABLE, "S003", "서비스를 일시적으로 사용할 수 없습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

}
