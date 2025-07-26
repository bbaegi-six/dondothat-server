package org.bbagisix.user.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VerificationStorageService {

	private final StringRedisTemplate redisTemplate;
	private static final String KEY_PREFIX = "verification-code:";

	public void saveCode(String email, String code) {
		redisTemplate.opsForValue().set(KEY_PREFIX + email, code, Duration.ofMinutes(5));
	}

	public String getCode(String email) {
		return redisTemplate.opsForValue().get(KEY_PREFIX + email);
	}

	public void removeCode(String email) {
		redisTemplate.delete(KEY_PREFIX + email);
	}
}
