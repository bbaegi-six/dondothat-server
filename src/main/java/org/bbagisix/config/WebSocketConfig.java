package org.bbagisix.config;

import org.bbagisix.chat.interceptior.WebSocketJwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 설정 - JWT 인증 및 CORS 강화
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final WebSocketJwtInterceptor webSocketJwtInterceptor;

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		// 메시지 브로커 설정
		config.enableSimpleBroker("/topic", "/queue");    // 구독 경로
		config.setApplicationDestinationPrefixes("/app");        // 메시지 전송 경로
		config.setUserDestinationPrefix("/user");        // 개인 메시지 경로

		log.info("[WebSocketConfig] 메시지 브로커 설정 완료");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// STOMP 엔드포인트 등록
		registry.addEndpoint("/ws/chat")
			.setAllowedOriginPatterns("*")        // CORS 설정 (구체적인 도메인으로 추후 변경)
			.withSockJS();                // SockJS fallback 옵션

		registry.addEndpoint("/ws/chat")
			.setAllowedOriginPatterns(
				"http://localhost:*",
				"http://127.0.0.1:*",
				"https://*.netlify.app",
				"http://dondothat.duckdns.org:*"
			)
			.withSockJS()
			.setHeartbeatTime(25000)    // 하트비트 주기 (클라이언트와 연결 상태 유지)
			.setDisconnectDelay(5000)   // 연결 종료까지 지연 시간
			.setSessionCookieNeeded(false); // SockJS 세션 쿠키 비활성화 (JWT 사용)

		log.info("[WebSocketConfig] STOMP 엔드포인트 등록 완료: /ws/chat");

	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		// 클라이언트 → 서버 메시지 채널: 인터셉터 추가로 인증 처리
		registration.interceptors(webSocketJwtInterceptor);
		registration.taskExecutor().corePoolSize(4);
		registration.taskExecutor().maxPoolSize(8);
		registration.taskExecutor().keepAliveSeconds(60);

		log.info("[WebSocketConfig] WebSocket JWT 인터셉터 등록 완료");
	}

	@Override
	public void configureClientOutboundChannel(ChannelRegistration registration) {
		// 서버에서 클라이언트로 가는 메시지 처리
		registration.taskExecutor().corePoolSize(4);
		registration.taskExecutor().maxPoolSize(8);
		registration.taskExecutor().keepAliveSeconds(60);

		log.info("[WebSocketConfig] 아웃바운드 채널 설정 완료");
	}
}
