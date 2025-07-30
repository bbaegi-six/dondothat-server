package org.bbagisix.chat.service;

import java.util.Map;

import org.bbagisix.chat.dto.ChatMessageDTO;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageSubscriber implements MessageListener {

	private final SimpMessagingTemplate messagingTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper()
		.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)    // 알 수 없는 필드 무시
		.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)    // ISO 문자열로 직렬화
		.findAndRegisterModules();

	@Override
	public void onMessage(Message message, byte[] pattern) {
		try {
			// 채널명 추출
			String channel = new String(message.getChannel());
			String messageBody = new String(message.getBody());

			log.debug("Redis에서 메시지 수신: channel={}, body={}", channel, messageBody);

			// JSON -> ChatMessageDTO (역직렬화)
			ChatMessageDTO chatMessage = objectMapper.readValue(messageBody, ChatMessageDTO.class);

			// 채널명에서 challengeId 추출
			Long challengeId = extractChallengeIdFromChannel(channel);

			if (challengeId == null) {
				log.warn("잘못된 채널 형식: {}", channel);
				return;
			}

			// 메시지 타입에 따른 처리
			handleMessage(challengeId, chatMessage);

		} catch (Exception e) {
			log.error("Redis 메시지 처리 중 오류 (무시하고 계속): ", e);
		}
	}

	/**
	 * 메시지 타입에 따른 처리
	 * @param challengeId
	 * @param chatMessage
	 */
	private void handleMessage(Long challengeId, ChatMessageDTO chatMessage) {
		try {
			if ("PARTICIPANT_COUNT".equals(chatMessage.getMessageType())) {
				int count = Integer.parseInt(chatMessage.getMessage());

				// 접속자 수 업데이트 메시지 - Map
				Map<String, Object> countMessage = Map.of(
					"type", "PARTICIPANT_COUNT",
					"challengeId", challengeId,
					"count", count
				);

				// 채팅 채널로 통합 전송 (프론트엔드에서 하나의 구독으로 처리)
				messagingTemplate.convertAndSend("/topic/chat/" + challengeId, countMessage);
				log.debug("접속자 수 업데이트 전송: challengeId={}, count={}", challengeId, count);

			} else {
				// 일반 채팅 메시지
				messagingTemplate.convertAndSend("/topic/chat/" + challengeId, chatMessage);
				log.debug("채팅 메시지 전송: challengeId={}, messageType={}", challengeId, chatMessage.getMessageType());
			}
		} catch (Exception e) {
			log.error("WebSocket 메시지 전송 중 오류 (무시하고 계속): challengeId={}", challengeId, e);
		}
	}

	/**
	 * 채널명에서 challengeId 추출
	 * "chat:channel:123" -> 123
	 */
	private Long extractChallengeIdFromChannel(String channel) {
		try {
			if (channel != null && channel.startsWith("chat:channel:")) {
				String idPart = channel.substring("chat:channel:".length());
				return Long.parseLong(idPart);
			}
		} catch (NumberFormatException e) {
			log.warn("challengeId 파싱 실패: channel={}", channel);
		}
		return null;
	}
}
