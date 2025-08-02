package org.bbagisix.user.service;

import org.bbagisix.user.domain.UserVO;
import org.bbagisix.user.dto.SignUpRequest;
import org.bbagisix.user.dto.SignUpResponse;
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

	public boolean isNicknameDuplicate(String nickname) {
		return userMapper.countByNickname(nickname) > 0;
	}

	public SignUpResponse findByUserId(Long userId) {
		UserVO userVO = userMapper.findByUserId(userId);
		if (userVO == null) {
			return null;
		}

		return SignUpResponse.builder()
			.name(userVO.getName())
			.email(userVO.getEmail())
			.nickname(userVO.getNickname())
			.role(userVO.getRole())
			.assetConnected(userVO.isAssetConnected())
			.build();
	}

	public SignUpResponse findByEmail(String email) {
		UserVO userVO = userMapper.findByEmail(email);
		if (userVO == null) {
			return null;
		}

		return SignUpResponse.builder()
			.name(userVO.getName())
			.email(userVO.getEmail())
			.nickname(userVO.getNickname())
			.role(userVO.getRole())
			.assetConnected(userVO.isAssetConnected())
			.build();
	}

	@Transactional
	public void signUp(SignUpRequest signUpRequest) {
		if (isNicknameDuplicate(signUpRequest.getNickname())) {
			throw new RuntimeException("이미 사용 중인 닉네임입니다.");
		}

		UserVO user = UserVO.builder()
			.name(signUpRequest.getName())
			.nickname(signUpRequest.getNickname())
			.password(signUpRequest.getPassword())
			.email(signUpRequest.getEmail())
			.emailVerified(false)
			.assetConnected(false)
			.role("USER")
			.build();

		userMapper.insertUser(user);
	}
}
