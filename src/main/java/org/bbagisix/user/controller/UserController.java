package org.bbagisix.user.controller;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.ibatis.annotations.Param;
import org.bbagisix.user.dto.LoginRequest;
import org.bbagisix.user.dto.SendCodeRequest;
import org.bbagisix.user.dto.SignUpRequest;
import org.bbagisix.user.dto.SignUpResponse;
import org.bbagisix.user.dto.CustomOAuth2User;
import org.bbagisix.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@PostMapping("/send-verification")
	public ResponseEntity<String> sendCode(@Valid @RequestBody SendCodeRequest request) {
		userService.sendVerificationCode(request.getEmail());
		return ResponseEntity.ok("인증 코드가 이메일로 발송되었습니다.");
	}

	@PostMapping("/signup")
	public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request, HttpServletResponse response) {
		SignUpResponse userInfo = userService.signUp(request, response);
		return ResponseEntity.status(HttpStatus.CREATED).body(userInfo);
	}

	@PostMapping("/login")
	public ResponseEntity<SignUpResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
		SignUpResponse userInfo = userService.login(request.getEmail(), request.getPassword(), response);
		return ResponseEntity.ok(userInfo);
	}

	@GetMapping("/check-email")
	public ResponseEntity<Boolean> checkUsername(@Param("email") String email) {
		return ResponseEntity.ok(userService.isEmailDuplicate(email));
	}

	@GetMapping("/me")
	public ResponseEntity<SignUpResponse> getCurrentUser(Authentication authentication) {
		SignUpResponse signUpResponse = userService.findByEmail(authentication.getName());
		return ResponseEntity.ok(signUpResponse);
	}

	@GetMapping("/profile")
	public ResponseEntity<SignUpResponse> getUserProfile(Authentication authentication) {
		// 인증된 사용자의 userId를 JWT에서 추출
		CustomOAuth2User currentUser = (CustomOAuth2User) authentication.getPrincipal();
		Long userId = currentUser.getUserId();
		
		// userId로 최신 사용자 정보 조회
		SignUpResponse userProfile = userService.findByUserId(userId);
		
		if (userProfile == null) {
			return ResponseEntity.notFound().build();
		}
		
		return ResponseEntity.ok(userProfile);
	}

	@PostMapping("/logout")
	public ResponseEntity<String> logout(HttpServletResponse response) {
		userService.logout(response);
		return ResponseEntity.ok("로그아웃이 완료되었습니다.");
	}
}
