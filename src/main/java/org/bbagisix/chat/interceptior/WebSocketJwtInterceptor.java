package org.bbagisix.chat.interceptior;

import java.util.List;
import java.util.Map;

import org.bbagisix.user.dto.CustomOAuth2User;
import org.bbagisix.user.dto.UserResponse;
import org.bbagisix.user.util.JwtUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket STOMP 연결 시 JWT 토큰 검증 인터셉터 (쿠키만 지원)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketJwtInterceptor implements ChannelInterceptor {

	private final JwtUtil jwtUtil;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

		if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
			// STOMP 연결 시 JWT 토큰 검증
			authenticateUser(accessor);
		}

		return message;
	}

	/**
	 * WebSocket 연결 시 JWT 토큰으로 사용자 인증
	 */
	private void authenticateUser(StompHeaderAccessor accessor) {
		try {
			String token = extractTokenFromCookie(accessor);

			if (token == null) {
				log.warn("❌ WebSocket 연결: JWT 토큰이 없습니다");
				return;
			}

			// JWT 토큰 검증
			if (!jwtUtil.isExpired(token)) {
				log.warn("❌ WebSocket 연결: 유효하지 않은 JWT 토큰");
				return;
			}

			// 토큰에서 사용자 정보 추출
			String email = jwtUtil.getEmail(token);
			String name = jwtUtil.getName(token);    // email 반환
			String nickname = jwtUtil.getNickname(token);
			Long userId = jwtUtil.getUserId(token);

			// 실제 이름은 nickname을 사용
			String displayName = nickname != null ? nickname : name;
			log.info("✅ WebSocket JWT 인증 성공: userId={}, email={}, nickname={}", userId, email, nickname);

			// UserResponse 객체 생성
			UserResponse userResponse = UserResponse.builder()
				.userId(userId)
				.email(email)
				.name(displayName)
				.nickname(nickname)
				.role("USER")
				.build();

			// CustomOAuth2User 객체 생성
			CustomOAuth2User customUser = new CustomOAuth2User(userResponse);

			// Authentication 객체 생성
			Authentication auth = new UsernamePasswordAuthenticationToken(
				customUser,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))
			);

			// WebSocket 세션에 인증 정보 저장
			accessor.setUser(auth);

			// 세션 속성에 사용자 정보 저장 (채팅에서 사용)
			Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
			if (sessionAttributes != null) {
				sessionAttributes.put("userId", userId);
				sessionAttributes.put("userName", displayName);    // nickname 사용
				sessionAttributes.put("email", email);
				sessionAttributes.put("authenticated", true);
			}

			log.info("📝 WebSocket 세션에 사용자 정보 저장 완료: userId={}", userId);
		} catch (Exception e) {
			log.error("❌ WebSocket JWT 인증 중 오류 발생: {}", e.getMessage(), e);
		}
	}

	/**
	 * Cookie에서 JWT 토큰 추출
	 */
	private String extractTokenFromCookie(StompHeaderAccessor accessor) {
		List<String> cookieHeaders = accessor.getNativeHeader("Cookie");
		if (cookieHeaders != null && !cookieHeaders.isEmpty()) {
			for (String cookieHeader : cookieHeaders) {
				String token = parseJwtFromCookie(cookieHeader);
				if (token != null) {
					log.debug("🍪 Cookie에서 JWT 토큰 추출 성공");
					return token;
				}
			}
		}

		return null;
	}

	/**
	 * Cookie 문자열에서 JWT 토큰 파싱
	 */
	private String parseJwtFromCookie(String cookieHeader) {
		if (cookieHeader == null)
			return null;

		String[] cookies = cookieHeader.split(";");
		for (String cookie : cookies) {
			String[] parts = cookie.trim().split("=", 2);    // key=value
			if (parts.length == 2 && "jwt".equals(parts[0].trim())) {
				return parts[1].trim();        // JWT 토큰 반환
			}
		}

		return null;
	}
}
