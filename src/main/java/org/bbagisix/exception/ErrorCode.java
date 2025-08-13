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
	SOCIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "E005", "지원하지 않는 소셜 로그인입니다."),

	// 챌린지 관련 에러
	CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CH001", "챌린지를 찾을 수 없습니다."),
	CHALLENGE_ID_REQUIRED(HttpStatus.BAD_REQUEST, "CH002", "챌린지 ID는 필수입니다."),
	CHALLENGE_ENDED(HttpStatus.BAD_REQUEST, "CH003", "종료된 챌린지입니다."),
	CHALLENGE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CH004", "해당 챌린지에 참여할 권한이 없습니다."),
	ALREADY_JOINED_CHALLENGE(HttpStatus.CONFLICT, "CH005", "이미 챌린지에 참여중입니다."),
	CHALLENGE_NOT_JOINED(HttpStatus.BAD_REQUEST, "CH006", "사용자 정보와 일치하는 챌린지가 없습니다"),
	CHALLENGE_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CH007", "챌린지 업데이트에 실패했습니다"),

	// 데이터베이스 관련 에러
	DATA_ACCESS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "D001", "일시적인 오류가 발생했습니다."),
	MESSAGE_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "D002", "메시지 저장에 실패했습니다."),
	MESSAGE_LOAD_FAILED(HttpStatus.INSUFFICIENT_STORAGE, "D003", "메시지를 불러올 수 없습니다."),

	// WebSocket 관련 에러
	WEBSOCKET_CONNECTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "W001", "채팅 연결에 실패했습니다."),
	WEBSOCKET_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "W002", "메시지 전송에 실패했습니다."),
	SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "W003", "세션이 만료되었습니다. 다시 연결해주세요."),

	// 소비내역 관련 에러
	EXPENSE_NOT_FOUND(HttpStatus.NOT_FOUND, "EX001", "소비내역을 찾을 수 없습니다."),
	EXPENSE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "EX002", "소비내역에 대한 접근 권한이 없습니다."),
	EXPENSE_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EX003", "소비내역 생성에 실패했습니다."),
	EXPENSE_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EX004", "소비내역 수정에 실패했습니다."),
	EXPENSE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EX005", "소비내역 삭제에 실패했습니다."),
	EXPENSE_SUMMARY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EX006", "지출 집계 조회에 실패했습니다."),

	// LLM 관련 에러
	LLM_CLASSIFY_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "L001", "카테고리 분류에 실패했습니다."),
	LLM_ANALYTICS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "L002", "과소비 카테고리 분석에 실패했습니다."),

	// 자산/계좌 관련 에러
	ASSET_FAIL(HttpStatus.BAD_REQUEST, "A001", "계좌 처리 중 오류가 발생했습니다."),
	ASSET_NOT_FOUND(HttpStatus.NOT_FOUND, "A002", "계좌를 찾을 수 없습니다."),
	ASSET_ALREADY_EXISTS(HttpStatus.CONFLICT, "A003", "이미 존재하는 계좌입니다."),

	// 거래내역 관련 에러
	TRANSACTION_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "T001", "거래내역 처리 중 오류가 발생했습니다."),
	TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "T002", "거래내역을 찾을 수 없습니다."),

	// Codef API 관련 에러
	CODEF_FAIL(HttpStatus.SERVICE_UNAVAILABLE, "CF001", "Codef API 처리 중 오류가 발생했습니다."),
	CODEF_AUTH_FAIL(HttpStatus.UNAUTHORIZED, "CF002", "Codef API 인증에 실패했습니다."),

	// 암호화 관련 에러
	ENCRYPTION_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "E001", "암호화 처리 중 오류가 발생했습니다."),

	// 금융상품 관련 에러
	FSS_API_CALL_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "FP001", "금감원 API 호출에 실패했습니다."),
	FSS_API_RESPONSE_ERROR(HttpStatus.BAD_GATEWAY, "FP002", "금감원 API 응답 처리 중 오류가 발생했습니다."),
	FINPRODUCT_DATA_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FP003", "금융상품 데이터 저장에 실패했습니다."),
	FINPRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "FP004", "금융상품을 찾을 수 없습니다."),

	// Authentication 관련 에러
	AUTHENTICATION_REQUIRED(HttpStatus.INTERNAL_SERVER_ERROR, "AU01", "Authentication인증이 필요합니다."),

	// 저금통 관련 에러
	SAVING_UPDATE_DENIED(HttpStatus.FORBIDDEN, "S001", "해당 금액을 저금할 권한이 없습니다."),
	SAVING_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S002", "저금통 계좌 잔액 업데이트에 실패했습니다."),

	// 일반적인 시스템 에러
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "서버 내부 오류가 발생했습니다."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "S002", "잘못된 요청입니다."),
	SERVICE_UNVALIABLE(HttpStatus.SERVICE_UNAVAILABLE, "S003", "서비스를 일시적으로 사용할 수 없습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

}
