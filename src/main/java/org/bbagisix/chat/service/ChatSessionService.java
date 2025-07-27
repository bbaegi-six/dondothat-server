package org.bbagisix.chat.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionService {

	private final SimpMessagingTemplate messagingTemplate;

	// challengeId 별 현재 접속자 수 저장 (메모리 기반)
	private final Map<Long, Integer> challengeParticipantCount = new ConcurrentHashMap<>();

	/**
	 * 사용자 입장 - 접속자 수 증가
	 */
	public void addParticipant(Long challengeId) {
		challengeParticipantCount.merge(challengeId, 1, Integer::sum);
		int currentCount = challengeParticipantCount.get(challengeId);

		log.info("챌린지 {} 접속자 증가: {} 명", challengeId, currentCount);

		// 접속자 수 변경을 모든 클라이언트에게 브로드캐스트
		broadcastParticipantCount(challengeId, currentCount);
	}

	/**
	 * 사용자 퇴장 - 접속자 수 감소
	 */
	public void removeParticipant(Long challengeId) {
		challengeParticipantCount.computeIfPresent(challengeId, (key, count) -> {    // challengeId가 존재할 때만 실행
			int newCount = Math.max(0, count - 1);    // 음수 방지 (비정상적 퇴장 방지)
			log.info("챌린지 {} 접속자 감소: {} 명", challengeId, newCount);

			// 접속자 수 변경을 모든 클라이언트에게 브로드캐스트
			broadcastParticipantCount(challengeId, newCount);

			return newCount == 0 ? null : newCount;        // 0이면 맵에서 제거 -> 메모리 효율성
		});
	}

	/**
	 * 현재 접속자 수 조회
	 */
	public int getParticipantCount(Long challengeId) {
		return challengeParticipantCount.getOrDefault(challengeId, 0);
	}

	/**
	 * 접속자 수를 모든 구독자에게 브로드캐스트
	 */
	public void broadcastParticipantCount(Long challengeId, int count) {
		Map<String, Object> countMessage = Map.of(
			"type", "PARTICIPANT_COUNT",
			"challengeId", challengeId,
			"count", count
		);

		messagingTemplate.convertAndSend("/topic/chat/" + challengeId, countMessage);
	}
}
