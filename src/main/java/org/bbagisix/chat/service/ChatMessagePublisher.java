package org.bbagisix.chat.service;

import org.bbagisix.chat.dto.ChatMessageDTO;
import org.bbagisix.common.exception.BusinessException;
import org.bbagisix.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessagePublisher {

	@Qualifier("chatRedisTemplate")
	private final RedisTemplate<String, Object> redisTemplate;    // pub/sub 메시지 발행 및 데이터 저장/조회

	private static final String CHAT_CHANNEL_PREFIX = "chat:channel:";    // 채팅방 별 격리와 패턴 매칭 위해 사용

	/**
	 * 특정 챌린지 채널로 메시지 발행
	 */
	public void publishMessage(Long challengeId, ChatMessageDTO message) {
		try {
			String channel = CHAT_CHANNEL_PREFIX + challengeId;
			log.debug("Redis로 메시지 발행: channel={}, message={}", channel, message.getMessage());

			// Redis pub/sub로 메시지 발행
			redisTemplate.convertAndSend(channel, message);
			log.debug("메시지 발행 완료: challengeId={}", challengeId);
		} catch (Exception e) {
			log.error("Redis 메시지 발행 중 오류: challengeId={}", challengeId, e);
			throw new BusinessException(ErrorCode.WEBSOCKET_SEND_FAILED, e);
		}
	}

	/**
	 * 접속자 수 변경 브로드캐스트
	 */
	public void publishParticipantCount(Long challengeId, int count) {
		try {
			String channel = CHAT_CHANNEL_PREFIX + challengeId;

			ChatMessageDTO countMessage = ChatMessageDTO.builder()
				.challengeId(challengeId)
				.messageType("PARTICIPANT_COUNT")
				.userId(0L)        // 시스템 메시지
				.message(String.valueOf(count))
				.build();

			log.debug("Redis로 접속자 수 발행: channel={}, count={}", channel, count);

			// Redis 발행
			redisTemplate.convertAndSend(channel, countMessage);
			log.debug("접속자 수 발행 완료: challengeId={}, count={}", challengeId, count);
		} catch (Exception e) {
			log.error("접속자 수 Redis 발행 중 오류: challengeId={}, count={}", challengeId, count, e);
			throw new BusinessException(ErrorCode.WEBSOCKET_SEND_FAILED, e);
		}
	}
}
