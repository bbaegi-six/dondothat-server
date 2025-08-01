package org.bbagisix.chat.converter;

import java.time.LocalDateTime;

import org.bbagisix.chat.domain.ChatMessageVO;
import org.bbagisix.chat.dto.ChatMessageDTO;
import org.bbagisix.chat.entity.ChatMessage;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageConverter {

	/**
	 * DTO -> VO
	 */
	public ChatMessageVO toVO(ChatMessageDTO dto) {
		if (dto == null) {
			return null;
		}

		return ChatMessageVO.builder()
			.challengeId(dto.getChallengeId())
			.userId(dto.getUserId())
			.message(dto.getMessage())
			.messageType(dto.getMessageType() != null ? dto.getMessageType() : "MESSAGE")
			.sentAt(dto.getSentAt() != null ? dto.getSentAt() : LocalDateTime.now())
			.userName(dto.getUserName())
			.build();
	}

	/**
	 * VO -> Entity
	 */
	public ChatMessage toEntity(ChatMessageVO vo) {
		if (vo == null) {
			return null;
		}

		return ChatMessage.builder()
			.challengeId(vo.getChallengeId())
			.userId(vo.getUserId())
			.message(vo.getMessage())
			.messageType(vo.getMessageType())
			.sentAt(vo.getSentAt())
			.userName(vo.getUserName())
			.build();
	}

	/**
	 * Entity → VO
	 */
	public ChatMessageVO fromEntity(ChatMessage entity) {
		if (entity == null) {
			return null;
		}

		return ChatMessageVO.builder()
			.challengeId(entity.getChallengeId())
			.userId(entity.getUserId())
			.message(entity.getMessage())
			.messageType(entity.getMessageType())
			.sentAt(entity.getSentAt())
			.userName(entity.getUserName())
			.build();
	}

	/**
	 * VO → DTO 변환 (응답용)
	 */
	public ChatMessageDTO toDTO(ChatMessageVO vo, Long messageId) {
		if (vo == null) {
			return null;
		}

		return ChatMessageDTO.builder()
			.messageId(messageId)
			.challengeId(vo.getChallengeId())
			.userId(vo.getUserId())
			.message(vo.getMessage())
			.messageType(vo.getMessageType())
			.sentAt(vo.getSentAt())
			.userName(vo.getUserName())
			.build();
	}

	/**
	 * Entity → DTO 변환 (조회용)
	 */
	public ChatMessageDTO toDTO(ChatMessage entity) {
		if (entity == null) {
			return null;
		}

		return ChatMessageDTO.builder()
			.messageId(entity.getMessageId())
			.challengeId(entity.getChallengeId())
			.userId(entity.getUserId())
			.message(entity.getMessage())
			.messageType(entity.getMessageType())
			.sentAt(entity.getSentAt())
			.userName(entity.getUserName())
			.build();
	}

	/**
	 * 시스템 메시지 생성 (입장/퇴장용)
	 */
	public ChatMessageVO createSystemMessage(Long challengeId, Long userId, String userName, String message) {
		return ChatMessageVO.builder()
			.challengeId(challengeId)
			.userId(userId)
			.userName(userName != null ? userName : "사용자" + userId)
			.message(message)
			.messageType("SYSTEM")
			.sentAt(LocalDateTime.now())
			.build();
	}

	/**
	 * 에러 메시지 생성
	 */
	public ChatMessageVO createErrorMessage(Long challengeId, String message) {
		return ChatMessageVO.builder()
			.challengeId(challengeId)
			.message(message)
			.messageType("ERROR")
			.sentAt(LocalDateTime.now())
			.build();
	}
}
