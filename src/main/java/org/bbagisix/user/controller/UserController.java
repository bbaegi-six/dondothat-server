package org.bbagisix.user.controller;

import java.util.Collections;

import javax.validation.Valid;

import org.bbagisix.user.dto.SendCodeRequest;
import org.bbagisix.user.dto.SignUpRequest;
import org.bbagisix.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/check-nickname")
	public ResponseEntity<?> checkNicknameDuplication(@RequestParam String nickname) {
		boolean isDuplicated = userService.checkNicknameDuplication(nickname);
		return ResponseEntity.ok(Collections.singletonMap("isDuplicated", isDuplicated));
	}

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
}
