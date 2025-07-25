package org.bbagisix.user.service;

import org.bbagisix.user.domain.UserVO;
import org.bbagisix.user.dto.SignUpRequest;
import org.bbagisix.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserMapper userMapper;
	private final EmailService emailService;
	private final VerificationStorageService verificationStorageService;

	public void sendVerificationCode(String email) {
		if (userMapper.countByEmail(email) > 0) {
			throw new RuntimeException("이미 가입된 이메일입니다.");
		}
		String code = emailService.generateVerificationCode();
		verificationStorageService.saveCode(email, code);
		emailService.sendVerificationCode(email, code);
	}

	@Transactional
	public void signUp(SignUpRequest signUpRequest) {
		String storedCode = verificationStorageService.getCode(signUpRequest.getEmail());

		log.info("Attempting to sign up for email: {}", signUpRequest.getEmail());
		log.info("Code from Postman: '{}'", signUpRequest.getVerificationCode());
		log.info("Code from Redis: '{}'", storedCode);

		if (storedCode == null || !storedCode.equals(signUpRequest.getVerificationCode())) {
			log.error("Verification code mismatch or not found.");
			throw new RuntimeException("인증 코드가 유효하지 않습니다.");
		}

		UserVO user = UserVO.builder()
			.name(signUpRequest.getName())
			.nickname(signUpRequest.getNickname())
			.password(signUpRequest.getPassword())
			.email(signUpRequest.getEmail())
			.emailVerified(true)
			.point(0L) // 👈 초기 포인트 값 설정
			.build();

		userMapper.insertUser(user);
		verificationStorageService.removeCode(signUpRequest.getEmail());
	}
}
