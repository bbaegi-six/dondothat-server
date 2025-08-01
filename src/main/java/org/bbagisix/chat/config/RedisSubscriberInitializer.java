package org.bbagisix.chat.config;

import org.bbagisix.chat.service.ChatMessageSubscriber;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriberInitializer {

	private final RedisMessageListenerContainer redisMessageListenerContainer;
	private final ChatMessageSubscriber chatMessageSubscriber;

	@PostConstruct
	public void initializeSubscribers() {
		try {
			PatternTopic chatChannelPattern = new PatternTopic("chat:channel:*");

			redisMessageListenerContainer.addMessageListener(
				chatMessageSubscriber,
				chatChannelPattern
			);

			log.info("✅ Redis 채팅 채널 구독 초기화 완료: pattern=chat:channel:*");

		} catch (Exception e) {
			log.error("❌ Redis 구독 초기화 실패: ", e);
			log.error("Redis 구독 설정 실패 - 채팅 기능이 제한될 수 있습니다.");
		}
	}
}
