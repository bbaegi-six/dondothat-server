package org.bbagisix.chat.controller;

import java.util.List;
import java.util.Map;

import org.bbagisix.chat.dto.ChatMessageDTO;
import org.bbagisix.chat.exception.BusinessException;
import org.bbagisix.chat.exception.ErrorCode;
import org.bbagisix.chat.service.ChatService;
import org.bbagisix.chat.service.ChatSessionService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class ChatController {

	private final ChatService chatService;
	private final ChatSessionService chatSessionService;
	private final SimpMessagingTemplate messagingTemplate;

	/**
	 * 참여중인 채팅방 목록 조회
	 */
	@GetMapping("/api/chat/user/{userId}")
	public Map<String, Object> getUserCurrentChatRoom(@PathVariable Long userId) {
		return chatService.getUserCurrentChatRoom(userId);
	}

	/**
	 * 특정 채팅방 정보 조회
	 */
	@GetMapping("/api/chat/{challengeId}/info")
	public Map<String, Object> getChatRoomInfo(@PathVariable Long challengeId) {
		int participantCount = chatSessionService.getParticipantCount(challengeId);

		return Map.of(
			"challengeId", challengeId,
			"challengeName", "챌린지 " + challengeId,        // TODO: 실제 챌린지 이름 조회
			"participantCount", participantCount,
			"status", "active"
		);
	}

	/**
	 * 채팅방 참여자 목록 조회
	 */
	@GetMapping("/api/chat/{challengeId}/participants")
	public List<Map<String, Object>> getParticipants(@PathVariable Long challengeId) {
		return chatService.getParticipants(challengeId);
	}

	/**
	 * 현재 접속자 수 조회
	 */
	@GetMapping("/api/chat/{challengeId}/participants/count")
	public Map<String, Object> getParticipantCount(@PathVariable Long challengeId) {
		int count = chatSessionService.getParticipantCount(challengeId);

		return Map.of(
			"challengeId", challengeId,
			"participantCount", count
		);
	}

	/**
	 * 채팅 메시지 전송 (WebSocket)
	 * /app/chat/{challengeId}/send 로 메시지를 받아서
	 * /topic/chat/{challengeId} 로 브로드캐스트
	 * DTO → VO → Entity
	 */
	@MessageMapping("/chat/{challengeId}/send")
	public void sendMessage(@DestinationVariable Long challengeId,
		@Payload ChatMessageDTO chatMessage,
		SimpMessageHeaderAccessor headerAccessor) {
		try {
			log.info("챌린지 {} 채팅 메시지 수신: {}", challengeId, chatMessage.getMessage());

			// 챌린지 ID 일치 검증
			if (!challengeId.equals(chatMessage.getChallengeId())) {
				log.warn("챌린지 ID 불일치: URL={}, DTO={}", challengeId, chatMessage.getChallengeId());
				chatMessage.setChallengeId(challengeId); // URL의 challengeId로 강제 설정
			}

			// 메시지를 VO → Entity 변환 후 DB에 저장
			ChatMessageDTO savedMessage = chatService.saveMessage(chatMessage);

			log.info("메시지 저장 완료: ID {}", savedMessage.getMessageId());

			// 해당 챌린지 구독자들에게 브로드캐스트
			messagingTemplate.convertAndSend("/topic/chat/" + challengeId, savedMessage);
		} catch (BusinessException e) {
			log.warn("비즈니스 예외 발생: code={}, message={}", e.getCode(), e.getMessage());
			// GlobalExceptionHandler 에서 처리
		} catch (Exception e) {
			log.error("메시지 전송 중 예상하지 못한 오류: ", e);
			throw new BusinessException(ErrorCode.WEBSOCKET_SEND_FAILED, e);
		}
	}

	/**
	 * 사용자가 채팅방에 입장 (WebSocket)
	 * 시스템이 제어하는 입장 처리
	 */
	@MessageMapping("/chat/{challengeId}/join")
	public void joinChat(@DestinationVariable Long challengeId,
		@Payload ChatMessageDTO joinMessage,
		SimpMessageHeaderAccessor headerAccessor) {

		// 기본값 설정 (null 방지)
		Long userId = joinMessage.getUserId() != null ? joinMessage.getUserId() : 0L;
		String userName = "사용자" + userId;

		try {
			log.info("📥 [입장 요청] 사용자 ID: {}, 챌린지 ID: {}", userId, challengeId);

			// ChatService를 통해 사용자 정보와 함께 입장 메시지 생성
			ChatMessageDTO systemMessage = chatService.handleJoin(challengeId, userId);

			// userName 안전성 체크
			if (systemMessage != null && systemMessage.getUserName() != null && !systemMessage.getUserName()
				.trim()
				.isEmpty()) {
				userName = systemMessage.getUserName();
			}

			// 세션에 정보 저장 - null 값 완전 차단
			saveToSession(headerAccessor, challengeId, userId, userName);

			// 접속자 수 증가
			chatSessionService.addParticipant(challengeId);
			int currentCount = chatSessionService.getParticipantCount(challengeId);

			log.info("✅ [입장 완료] 사용자: {}, 챌린지: {}, 현재 접속자 수: {}명", userName, challengeId, currentCount);

			// 시스템 메시지가 null일 경우 에러 처리
			if (systemMessage == null) {
				throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "입장 메시지 생성에 실패했습니다.");
			}

			// 시스템 메시지는 DB에 저장하지 않고 바로 브로드캐스트
			messagingTemplate.convertAndSend("/topic/chat/" + challengeId, systemMessage);

			// 접속자 수 브로드캐스트
			messagingTemplate.convertAndSend("/topic/userCount/" + challengeId, currentCount);

			log.info("입장 처리 완료: 사용자 {}, 챌린지 {}", userName, challengeId);

		} catch (BusinessException e) {
			log.warn("❌ [입장 실패] 비즈니스 예외: code={}, message={}, 사용자: {}", e.getCode(), e.getMessage(), userId);
			handleJoinError(challengeId, userId, userName, headerAccessor);
			// GlobalExceptionHandler에서 처리
		} catch (Exception e) {
			log.error("❌ [입장 실패] 예상하지 못한 오류: 사용자: {}, 챌린지: {}", userId, challengeId, e);
			handleJoinError(challengeId, userId, userName, headerAccessor);
			throw new BusinessException(ErrorCode.WEBSOCKET_CONNECTION_ERROR, e);
		}
	}

	/**
	 * WebSocket 연결 해제 시 자동 호출
	 * 시스템이 제어하는 퇴장 처리
	 */
	@EventListener
	public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
		try {
			SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());

			Long challengeId = (Long)headerAccessor.getSessionAttributes().get("challengeId");
			Long userId = (Long)headerAccessor.getSessionAttributes().get("userId");
			String userName = (String)headerAccessor.getSessionAttributes().get("userName");

			log.info("🔌 [WebSocket 연결 해제] 세션 ID: {}, challengeId: {}, userId: {}, userName: {}",
				headerAccessor.getSessionId(), challengeId, userId, userName);

			if (challengeId != null) {
				log.info("👋 [퇴장 시작] 사용자: {}, 챌린지: {}", userName, challengeId);

				// 접속자 수 감소
				chatSessionService.removeParticipant(challengeId);
				int currentCount = chatSessionService.getParticipantCount(challengeId);

				log.info("✅ [퇴장 완료] 사용자: {}, 챌린지: {}, 현재 접속자 수: {}명", userName, challengeId, currentCount);

				// 퇴장 메시지 전송 (VO 기반)
				if (userName != null) {
					ChatMessageDTO systemMessage = chatService.handleLeave(challengeId, userId, userName);
					messagingTemplate.convertAndSend("/topic/chat/" + challengeId, systemMessage);
				}

				// 접속자 수 브로드캐스트
				messagingTemplate.convertAndSend("/topic/userCount/" + challengeId, currentCount);
			}
		} catch (BusinessException e) {
			log.warn("퇴장 처리 중 비즈니스 예외: code={}, message={}", e.getCode(), e.getMessage());
		} catch (Exception e) {
			log.error("WebSocket 연결 해제 처리 중 예상하지 못한 오류: ", e);
		}
	}

	/**
	 * 입장 오류 처리 (공통 메서드)
	 */
	private void handleJoinError(Long challengeId, Long userId, String userName,
		SimpMessageHeaderAccessor headerAccessor) {
		try {
			// 에러 발생 시에도 접속자 수는 증가
			chatSessionService.addParticipant(challengeId);

			// 안전한 세션 저장
			saveToSession(headerAccessor, challengeId, userId, userName);
		} catch (Exception e) {
			log.error("입장 오류 처리 중 추가 오류: ", e);
		}
	}

	/**
	 * 세션 저장 (공통 메서드)
	 */
	private void saveToSession(SimpMessageHeaderAccessor headerAccessor, Long challengeId, Long userId,
		String userName) {
		try {
			Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
			if (sessionAttributes != null) {
				sessionAttributes.put("challengeId", challengeId);
				sessionAttributes.put("userId", userId);
				sessionAttributes.put("userName", userName != null ? userName : "사용자" + userId);
			}
		} catch (Exception e) {
			log.error("세션 저장 중 오류: ", e);
			throw new BusinessException(ErrorCode.SESSION_EXPIRED, e);
		}
	}
}