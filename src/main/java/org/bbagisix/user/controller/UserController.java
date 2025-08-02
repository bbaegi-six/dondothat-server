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
	public ResponseEntity<String> signUp(@Valid @RequestBody SignUpRequest request, HttpServletResponse response) {
		userService.signUp(request, response);
		return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 성공적으로 완료되었습니다.");
	}

	@PostMapping("/login")
	public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
		userService.login(request.getEmail(), request.getPassword(), response);
		return ResponseEntity.ok("로그인이 완료되었습니다.");
	}

	@GetMapping("/check-email")
	public ResponseEntity<Boolean> checkEmail(@Param("email") String email) {
		return ResponseEntity.ok(userService.isEmailDuplicate(email));
	}

	@GetMapping("/me")
	public ResponseEntity<SignUpResponse> getCurrentUser(Authentication authentication) {
		// JWT에서 userId 추출하여 최신 정보 조회
		CustomOAuth2User currentUser = (CustomOAuth2User) authentication.getPrincipal();
		Long userId = currentUser.getUserId();
		
		SignUpResponse userInfo = userService.findByUserId(userId);
		
		if (userInfo == null) {
			return ResponseEntity.notFound().build();
		}
		
		return ResponseEntity.ok(userInfo);
	}

	@PostMapping("/logout")
	public ResponseEntity<String> logout(HttpServletResponse response) {
		userService.logout(response);
		return ResponseEntity.ok("로그아웃이 완료되었습니다.");
	}
}
