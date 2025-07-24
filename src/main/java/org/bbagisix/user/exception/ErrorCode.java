package org.bbagisix.user.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// 사용자 관련 에러
	USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	EMAIL_ALREADY_EXISTS("U002", "이미 사용 중인 이메일입니다.", HttpStatus.BAD_REQUEST),
	INVALID_VERIFICATION_CODE("U003", "인증코드가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
	VERIFICATION_CODE_EXPIRED("U004", "인증코드가 만료되었습니다.", HttpStatus.BAD_REQUEST),

	// 이메일 관련 에러
	EMAIL_SEND_FAILED("E001", "이메일 발송에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

	// 공통 에러
	INTERNAL_SERVER_ERROR("C001", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
	INVALID_INPUT("C002", "입력값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST);

	private final String code;
	private final String message;
	private final HttpStatus httpStatus;


}
