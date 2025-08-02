package org.bbagisix.user.service;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;

import org.bbagisix.user.domain.UserVO;
import org.bbagisix.user.dto.CustomOAuth2User;
import org.bbagisix.user.dto.SignUpRequest;
import org.bbagisix.user.dto.UserResponse;
import org.bbagisix.user.mapper.UserMapper;
import org.bbagisix.user.util.CookieUtil;
import org.bbagisix.user.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
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
	private final JwtUtil jwtUtil;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public UserResponse getCurrentUser(Authentication authentication) {
		CustomOAuth2User currentUser = (CustomOAuth2User) authentication.getPrincipal();
		UserVO userVO = userMapper.findByUserId(currentUser.getUserId());

		return UserResponse.builder()
			.userId(userVO.getUserId())
			.name(userVO.getName())
			.email(userVO.getEmail())
			.nickname(userVO.getNickname())
			.role(userVO.getRole())
			.assetConnected(userVO.isAssetConnected())
			.build();
	}

	@Transactional
	public boolean isEmailDuplicate(String email) {
		return userMapper.countByEmail(email) > 0;
	}

	@Transactional
	public String updateNickname(Authentication authentication, String nickname) {
		CustomOAuth2User currentUser = (CustomOAuth2User) authentication.getPrincipal();
		userMapper.updateNickname(currentUser.getUserId(), nickname);
		return "닉네임 변경 완료";
	}

	@Transactional
	public String signUp(SignUpRequest signUpRequest, HttpServletResponse response) {
		if (isEmailDuplicate(signUpRequest.getEmail())) {
			return "이미 사용 중인 이메일입니다.";
		}

		String encodedPassword = passwordEncoder.encode(signUpRequest.getPassword());

		UserVO user = UserVO.builder()
			.name(signUpRequest.getName())
			.nickname(signUpRequest.getNickname())
			.password(encodedPassword) // 암호화된 비밀번호 저장
			.email(signUpRequest.getEmail())
			.emailVerified(false)
			.assetConnected(false)
			.role("USER")
			.build();

		userMapper.insertUser(user);
		
		String token = jwtUtil.createToken(
			user.getEmail(), 
			user.getRole(), 
			user.getName(), 
			user.getNickname(),
			user.getUserId(),
			24 * 60 * 60 * 1000L
		);
		
		CookieUtil.addJwtCookie(response, token);
		return "회원가입 완료.";
	}

	@Transactional
	public String login(String email, String password, HttpServletResponse response) {
		UserVO user = userMapper.findByEmail(email);
		if (user == null) {
			return "존재하지 않는 이메일입니다.";
		}

		if (!passwordEncoder.matches(password, user.getPassword())) {
			return "비밀번호가 일치하지 않습니다.";
		}

		// JWT 토큰 생성
		String token = jwtUtil.createToken(
			user.getEmail(),
			user.getRole(),
			user.getName(),
			user.getNickname(),
			user.getUserId(),
			24 * 60 * 60 * 1000L
		);

		// JWT 토큰을 HttpOnly 쿠키로 설정
		CookieUtil.addJwtCookie(response, token);

		return "로그인 성공.";
	}

	@Transactional
	public String logout(HttpServletResponse response, HttpServletRequest request) {
		CookieUtil.deleteJwtCookie(response);
		Cookie jsessionCookie = new Cookie("JSESSIONID", "");
		jsessionCookie.setPath("/");
		jsessionCookie.setMaxAge(0);
		response.addCookie(jsessionCookie);
		
		// JSESSIONID 삭제
		response.addHeader("Set-Cookie", 
			"JSESSIONID=" + 
			"; Path=/" + 
			"; Max-Age=0" + 
			"; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
		
		// 세션 무효화
		if (request.getSession(false) != null) {
			request.getSession().invalidate();
		}

		return "로그아웃 완료.";
	}

	@Transactional
	public void processOAuth2Login(String email, String role, String name, String nickname, Long userId, HttpServletResponse response) {
		log.info("OAuth2 로그인 처리 시작: {}", email);
		String token = jwtUtil.createToken(email, role, name, nickname, userId, 24 * 60 * 60 * 1000L);
		CookieUtil.addJwtCookie(response, token);
		log.info("OAuth2 로그인 JWT 쿠키 설정 완료: {}", email);
	}

	@Transactional
	public String sendVerificationCode(String email) {
		if (userMapper.countByEmail(email) > 0) {
			return "이미 사용 중인 이메일입니다.";
		}
		String code = emailService.generateVerificationCode();
		verificationStorageService.saveCode(email, code);
		emailService.sendVerificationCode(email, code);
		return "인증코드 발송 완료";
	}
}
