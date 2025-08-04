package org.bbagisix.config;

import org.bbagisix.chat.service.ChatMessageSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

	private final RedisConnectionFactory redisConnectionFactory;

	@Bean(name = "chatRedisTemplate")
	@Primary
	public RedisTemplate<String, Object> redisTemplate() {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(redisConnectionFactory);

		// Jackson2JsonRedisSerializer 설정
		Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(
			Object.class);

		// ObjectMapper 설정 (JSR310 모듈 등록)
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		serializer.setObjectMapper(objectMapper);

		template.setDefaultSerializer(serializer);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(serializer);
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setHashKeySerializer(serializer);

		return template;
	}

	@Bean
	public RedisMessageListenerContainer redisMessageListenerContainer(
		ChatMessageSubscriber chatMessageSubscriber) {  // 의존성 주입 추가

		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(redisConnectionFactory);

		container.addMessageListener(chatMessageSubscriber,
			new PatternTopic("chat:channel:*"));

		return container;
	}

	@Bean
	public RedisMessageListenerContainer redisMessageListenerContainer() {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(redisConnectionFactory);
		return container;
	}
}
