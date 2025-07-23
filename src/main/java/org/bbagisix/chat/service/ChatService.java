package org.bbagisix.chat.service;

import java.sql.Timestamp;
import java.util.Map;

import org.bbagisix.chat.dto.ChatMessageDTO;
import org.bbagisix.chat.mapper.ChatMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

	private final ChatMapper chatMapper;

	/**
	 * 채팅 메시지 저장
	 */
	public ChatMessageDTO saveMessage(ChatMessageDTO chatMessage) {
		log.debug("채팅 메시지 저장: {}", chatMessage);

		// 현재 시간 설정
		if (chatMessage.getSentAt() == null) {
			chatMessage.setSentAt(new java.sql.Timestamp(System.currentTimeMillis()));
		}

		// 메시지 타입이 없으면 기본 설정
		if (chatMessage.getMessageType() == null) {
			chatMessage.setMessageType("MESSAGE");
		}

		// DB 에 저장
		int result = chatMapper.insertMessage(chatMessage);
		if (result != 1) {
			throw new RuntimeException("메시지 저장에 실패했습니다.");
		}

		// 저장된 메시지 정보와 함께 사용자 정보 가져오기
		ChatMessageDTO savedMessage = chatMapper.selectMessageById(chatMessage.getMessageId());

		log.debug("메시지 저장 완료: ID {}", savedMessage.getMessageId());
		return savedMessage;
	}

	/**
	 * 사용자 입장 처리
	 */
	public ChatMessageDTO handleJoin(Long challengeId, Long userId) {
		// null 체크
		if (challengeId == null || userId == null) {
			log.warn("입장 처리 실패: challengeId={}, userId={}", challengeId, userId);
			return null;
		}

		String userName = "사용자" + userId; // 기본값
		String profileImage = null;

		try {
			// 사용자 정보 조회 시도
			Map<String, Object> userMap = chatMapper.selectUserById(userId);

			if (userMap != null && !userMap.isEmpty()) {
				String dbUserName = (String)userMap.get("user_name");
				if (dbUserName != null && !dbUserName.trim().isEmpty()) {
					userName = dbUserName;
				}
				profileImage = (String)userMap.get("profile_image");
				log.debug("사용자 정보 조회 성공: ID {}, 이름 {}", userId, userName);
			} else {
				log.warn("사용자 ID {}의 정보를 DB에서 찾을 수 없습니다. 기본값 사용: {}", userId, userName);
			}
		} catch (Exception e) {
			log.error("사용자 정보 조회 중 오류 (ID: {}): {}", userId, e.getMessage());
		}

		return ChatMessageDTO.builder()
			.challengeId(challengeId)
			.userId(userId)
			.userName(userName)
			.userProfileImage(profileImage)
			.message(userName + "님이 입장했습니다.")
			.messageType("SYSTEM")
			.sentAt(new Timestamp(System.currentTimeMillis()))
			.build();
	}
}
