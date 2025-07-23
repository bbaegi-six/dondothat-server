package org.bbagisix.chat.service;

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
}
