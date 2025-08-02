package org.bbagisix.user.service;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;

import org.bbagisix.user.domain.UserVO;
import org.bbagisix.user.dto.SignUpRequest;
import org.bbagisix.user.dto.SignUpResponse;
import org.bbagisix.user.mapper.UserMapper;
import org.bbagisix.user.util.CookieUtil;
import org.bbagisix.user.util.JwtUtil;
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

	public void sendVerificationCode(String email) {
		if (userMapper.countByEmail(email) > 0) {
			throw new RuntimeException("이미 가입된 이메일입니다.");
		}
		String code = emailService.generateVerificationCode();
		verificationStorageService.saveCode(email, code);
		emailService.sendVerificationCode(email, code);
	}

	public boolean isEmailDuplicate(String email) {
		return userMapper.countByEmail(email) > 0;
	}

	public SignUpResponse findByUserId(Long userId) {
		UserVO userVO = userMapper.findByUserId(userId);
		if (userVO == null) {
			return null;
		}

		return SignUpResponse.builder()
			.userId(userVO.getUserId())
			.name(userVO.getName())
			.email(userVO.getEmail())
			.nickname(userVO.getNickname())
			.role(userVO.getRole())
			.assetConnected(userVO.isAssetConnected())
			.build();
	}

	@Transactional
	public void signUp(SignUpRequest signUpRequest, HttpServletResponse response) {
		// 이메일 중복 체크
		if (isEmailDuplicate(signUpRequest.getEmail())) {
			throw new RuntimeException("이미 가입된 이메일입니다.");
		}

		// 비밀번호 암호화
		String encodedPassword = passwordEncoder.encode(signUpRequest.getPassword());
		log.info("비밀번호 암호화 완료 - 원본 길이: {}, 암호화 후 길이: {}", 
			signUpRequest.getPassword().length(), encodedPassword.length());

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
		
		// JWT 토큰 생성 및 쿠키 설정 (자동 로그인)
		String token = jwtUtil.createToken(
			user.getEmail(), 
			user.getRole(), 
			user.getName(), 
			user.getNickname(),
			user.getUserId(),
			24 * 60 * 60 * 1000L
		);
		
		CookieUtil.addJwtCookie(response, token);
		log.info("회원가입 및 자동 로그인 완료: {}", user.getEmail());
	}

	public void logout(HttpServletResponse response, HttpServletRequest request) {
		log.info("로그아웃 처리 시작");
		
		// JWT 쿠키 삭제
		CookieUtil.deleteJwtCookie(response);
		
		// JSESSIONID 쿠키도 삭제
		Cookie jsessionCookie = new Cookie("JSESSIONID", "");
		jsessionCookie.setPath("/");
		jsessionCookie.setMaxAge(0);
		response.addCookie(jsessionCookie);
		
		// Set-Cookie 헤더로도 JSESSIONID 삭제
		response.addHeader("Set-Cookie", 
			"JSESSIONID=" + 
			"; Path=/" + 
			"; Max-Age=0" + 
			"; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
		
		// 세션 무효화 (혹시 세션이 있다면)
		if (request.getSession(false) != null) {
			request.getSession().invalidate();
			log.info("세션 무효화 완료");
		}
		
		log.info("로그아웃 처리 완료 - JWT 쿠키 및 세션 삭제");
	}

	public void login(String email, String password, HttpServletResponse response) {
		log.info("로그인 처리 시작: {}", email);
		
		// 사용자 조회
		UserVO user = userMapper.findByEmail(email);
		if (user == null) {
			throw new RuntimeException("존재하지 않는 이메일입니다.");
		}
		
		// 암호화된 비밀번호와 비교
		if (!passwordEncoder.matches(password, user.getPassword())) {
			log.warn("비밀번호 불일치 - 이메일: {}", email);
			throw new RuntimeException("비밀번호가 일치하지 않습니다.");
		}
		
		log.info("비밀번호 검증 성공: {}", email);
		
		// JWT 토큰 생성 (24시간 유효)
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
		
		log.info("로그인 성공: {}", email);
	}

	public void processOAuth2Login(String email, String role, String name, String nickname, Long userId, HttpServletResponse response) {
		log.info("OAuth2 로그인 처리 시작: {}", email);
		String token = jwtUtil.createToken(email, role, name, nickname, userId, 24 * 60 * 60 * 1000L);
		CookieUtil.addJwtCookie(response, token);
		log.info("OAuth2 로그인 JWT 쿠키 설정 완료: {}", email);
	}
}
