package org.bbagisix.chat.controller;

import java.util.Map;

import org.bbagisix.chat.dto.ChatMessageDTO;
import org.bbagisix.chat.service.ChatService;
import org.bbagisix.chat.service.ChatSessionService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;
	private final ChatSessionService chatSessionService;
	private final SimpMessagingTemplate messagingTemplate;

	@GetMapping("/chat")
	public String chatTestPage() {
		return "chat-test";
	}

	/**
	 * 채팅 메시지 전송
	 * /app/chat/{challengeId}/send 로 메시지를 받아서
	 * /topic/chat/{challengeId} 로 브로드캐스트
	 */
	@MessageMapping("/chat/{challengeId}/send")
	public void sendMessage(@DestinationVariable Long challengeId,
		@Payload ChatMessageDTO chatMessage,
		SimpMessageHeaderAccessor headerAccessor) {
		try {
			log.info("챌린지 {} 채팅 메시지 수신: {}", challengeId, chatMessage.getMessage());

			// 세션에서 사용자 정보 가져오기 (나중에 인증 구현 시 사용)
			String sessionId = headerAccessor.getSessionId();

			// 메시지를 DB에 저장
			chatMessage.setChallengeId(challengeId);
			ChatMessageDTO savedMessage = chatService.saveMessage(chatMessage);

			log.info("메시지 저장 완료: ID {}", savedMessage.getMessageId());

			// 해당 챌린지 구독자들에게 브로드캐스트
			messagingTemplate.convertAndSend("/topic/chat/" + challengeId, savedMessage);
		} catch (Exception e) {
			log.error("메시지 전송 중 오류: ", e);

			// 에러 메시지 클라이언트에게 전송
			ChatMessageDTO errorMessage = ChatMessageDTO.builder()
				.challengeId(challengeId)
				.message("메시지 전송에 실패했습니다.")
				.messageType("ERROR")
				.build();
			messagingTemplate.convertAndSend("/topic/chat/" + challengeId, errorMessage);
		}
	}

	/**
	 * 사용자가 채팅방에 입장
	 */
	@MessageMapping("/chat/{challengeId}/join")
	public void joinChat(@DestinationVariable Long challengeId,
		@Payload ChatMessageDTO joinMessage,
		SimpMessageHeaderAccessor headerAccessor) {

		// 기본값 설정 (null 방지)
		Long userId = joinMessage.getUserId() != null ? joinMessage.getUserId() : 0L;
		String userName = "사용자" + userId;

		try {
			log.info("사용자 {} 챌린지 {} 채팅방에 입장", userId, challengeId);

			// ChatService를 통해 사용자 정보와 함께 입장 메시지 생성
			ChatMessageDTO systemMessage = chatService.handleJoin(challengeId, userId);

			// userName 안전성 체크
			if (systemMessage != null && systemMessage.getUserName() != null && !systemMessage.getUserName()
				.trim()
				.isEmpty()) {
				userName = systemMessage.getUserName();
			}

			// 세션에 정보 저장 - null 값 완전 차단
			Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
			if (sessionAttributes != null) {
				sessionAttributes.put("challengeId", challengeId);
				sessionAttributes.put("userId", userId);
				sessionAttributes.put("userName", userName);
			}

			// 접속자 수 증가
			chatSessionService.addParticipant(challengeId);

			// 시스템 메시지 생성 (systemMessage가 null일 경우 대비)
			ChatMessageDTO finalSystemMessage = systemMessage != null ? systemMessage :
				ChatMessageDTO.builder()
					.challengeId(challengeId)
					.userId(userId)
					.userName(userName)
					.message(userName + "님이 입장했습니다.")
					.messageType("SYSTEM")
					.build();

			// 시스템 메시지는 DB에 저장하지 않고 바로 브로드캐스트
			messagingTemplate.convertAndSend("/topic/chat/" + challengeId, finalSystemMessage);

			log.info("입장 처리 완료: 사용자 {}, 챌린지 {}", userName, challengeId);

		} catch (Exception e) {
			log.error("채팅방 입장 중 오류: ", e);

			// 에러 발생 시에도 접속자 수는 증가
			chatSessionService.addParticipant(challengeId);

			// 안전한 세션 저장
			try {
				Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
				if (sessionAttributes != null) {
					sessionAttributes.put("challengeId", challengeId);
					sessionAttributes.put("userId", userId);
					sessionAttributes.put("userName", userName);
				}
			} catch (Exception sessionEx) {
				log.error("세션 저장 중 추가 오류: ", sessionEx);
			}

			// 에러 메시지 전송
			ChatMessageDTO errorMessage = ChatMessageDTO.builder()
				.challengeId(challengeId)
				.message("입장 처리 중 오류가 발생했습니다.")
				.messageType("ERROR")
				.build();
			messagingTemplate.convertAndSend("/topic/chat/" + challengeId, errorMessage);
		}
	}

	/**
	 * WebSocket 연결 해제 시 자동 호출
	 */
	@EventListener
	public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
		try {
			SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());

			Long challengeId = (Long)headerAccessor.getSessionAttributes().get("challengeId");
			String userName = (String)headerAccessor.getSessionAttributes().get("userName");

			if (challengeId != null) {
				log.info("사용자 {}가 챌린지 {} 채팅방에서 퇴장", userName, challengeId);

				// 접속자 수 감소
				chatSessionService.removeParticipant(challengeId);

				// 퇴장 메시지 전송
				if (userName != null) {
					ChatMessageDTO systemMessage = ChatMessageDTO.builder()
						.challengeId(challengeId)
						.message(userName + "님이 퇴장했습니다.")
						.messageType("SYSTEM")
						.build();

					messagingTemplate.convertAndSend("/topic/chat/" + challengeId, systemMessage);
				}
			}
		} catch (Exception e) {
			log.error("WebSocket 연결 해제 처리 중 오류: ", e);
		}
	}
}