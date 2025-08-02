package org.bbagisix.user.controller;

import javax.validation.Valid;

import org.apache.ibatis.annotations.Param;
import org.bbagisix.user.dto.SendCodeRequest;
import org.bbagisix.user.dto.SignUpRequest;
import org.bbagisix.user.dto.SignUpResponse;
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
	public ResponseEntity<String> signUp(@Valid @RequestBody SignUpRequest request) {
		userService.signUp(request);
		return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 성공적으로 완료되었습니다.");
	}

	@GetMapping("/check-nickname")
	public ResponseEntity<Boolean> checkUsername(@Param("nickname") String nickname) {
		return ResponseEntity.ok(userService.isNicknameDuplicate(nickname));
	}

	@GetMapping("/me")
	public ResponseEntity<SignUpResponse> getCurrentUser(Authentication authentication) {
		SignUpResponse signUpResponse = userService.findByEmail(authentication.getName());
		return ResponseEntity.ok(signUpResponse);
	}
}
