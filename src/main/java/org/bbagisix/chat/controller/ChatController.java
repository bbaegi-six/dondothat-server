package org.bbagisix.chat.controller;

import org.bbagisix.chat.dto.ChatMessageDTO;
import org.bbagisix.chat.service.ChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;
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
		try {
			log.info("사용자가 챌린지 {} 채팅방에 입장", challengeId);

			// 입장 메시지 생성
			ChatMessageDTO systemMessage = ChatMessageDTO.builder()
				.challengeId(challengeId)
				.userId(joinMessage.getUserId())
				.message(joinMessage.getUserName() + "님이 입장했습니다.")
				.messageType("SYSTEM")
				.build();

			// 시스템 메시지는 DB에 저장하지 않고 바로 브로드캐스트
			messagingTemplate.convertAndSend("/topic/chat/" + challengeId, systemMessage);
		} catch (Exception e) {
			log.error("채팅방 입장 중 오류: ", e);
		}
	}
}