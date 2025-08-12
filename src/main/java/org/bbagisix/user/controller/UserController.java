package org.bbagisix.user.controller;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.ibatis.annotations.Param;
import org.bbagisix.user.dto.LoginRequest;
import org.bbagisix.user.dto.SendCodeRequest;
import org.bbagisix.user.dto.SignUpRequest;
import org.bbagisix.user.dto.UserResponse;
import org.bbagisix.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
		return ResponseEntity.ok(userService.sendVerificationCode(request.getEmail()));
	}

	@PostMapping("/signup")
	public ResponseEntity<String> signUp(@Valid @RequestBody SignUpRequest request, HttpServletResponse response) {
		return ResponseEntity.ok(userService.signUp(request, response));
	}

	@PostMapping("/login")
	public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
		return ResponseEntity.ok(userService.login(request.getEmail(), request.getPassword(), response));
	}

	@GetMapping("/check-email")
	public ResponseEntity<Boolean> checkEmail(@Param("email") String email) {
		return ResponseEntity.ok(userService.isEmailDuplicate(email));
	}

	@PutMapping("/update-nickname")
	public ResponseEntity<String> updateNickname(@Param("nickname") String nickname, Authentication authentication) {
		return ResponseEntity.ok(userService.updateNickname(authentication, nickname));
	}

	@PutMapping("/update-assetConnected")
	public ResponseEntity<String> updateAssetConnected(@Param("assetConnected") Boolean assetConnected,
		Authentication authentication) {
		return ResponseEntity.ok(userService.updateAssetConnected(assetConnected, authentication));
	}

	@PutMapping("/update-savingConnected")
	public ResponseEntity<String> updateSavingConnected(@Param("savingConnected") Boolean savingConnected, Authentication authentication) {
		return ResponseEntity.ok(userService.updateSavingConnected(savingConnected, authentication));
	}

	@GetMapping("/me")
	public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {

		return ResponseEntity.ok(userService.getCurrentUser(authentication));
	}

	@PostMapping("/logout")
	public ResponseEntity<String> logout(HttpServletResponse response, HttpServletRequest request) {
		return ResponseEntity.ok(userService.logout(response, request));
	}
}
