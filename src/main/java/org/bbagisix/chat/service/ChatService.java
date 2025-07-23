package org.bbagisix.chat.service;

import java.sql.Timestamp;
import java.util.Map;

import org.bbagisix.chat.converter.ChatMessageConverter;
import org.bbagisix.chat.domain.ChatMessageVO;
import org.bbagisix.chat.dto.ChatMessageDTO;
import org.bbagisix.chat.entity.ChatMessage;
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
	private final ChatMessageConverter converter;

	/**
	 * 채팅 메시지 저장
	 */
	public ChatMessageDTO saveMessage(ChatMessageDTO dto) {
		log.info("챌린지 ID: {}", dto.getChallengeId());
		log.info("유저 ID: {}", dto.getUserId());

		log.debug("채팅 메시지 저장 요청: challengeId={}, userId={}, message={}",
			dto.getChallengeId(), dto.getUserId(), dto.getMessage());

		try {
			// 1. DTO → VO 변환
			ChatMessageVO vo = converter.toVO(dto);

			// 2. 비즈니스 로직 검증
			validateMessage(vo);

			// 3. VO → Entity 변환
			ChatMessage entity = converter.toEntity(vo);

			// 4. DB 저장
			int result = chatMapper.insertMessage(entity);
			if (result != 1) {
				throw new RuntimeException("메시지 저장에 실패했습니다.");
			}

			// 5. 저장된 메시지 조회 (사용자 정보 포함)
			ChatMessage savedEntity = chatMapper.selectMessageById(entity.getMessageId());

			// 6. Entity → DTO 변환하여 반환
			ChatMessageDTO resultDTO = converter.toDTO(savedEntity);

			log.debug("메시지 저장 완료: messageId={}", resultDTO.getMessageId());
			return resultDTO;

		} catch (IllegalArgumentException e) {
			log.warn("메시지 검증 실패: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("메시지 저장 중 오류: ", e);
			throw new RuntimeException("메시지 저장 중 오류가 발생했습니다.", e);
		}
	}

	/**
	 * 사용자 입장 처리
	 */
	public ChatMessageDTO handleJoin(Long challengeId, Long userId) {
		log.debug("사용자 입장 처리: challengeId={}, userId={}", challengeId, userId);

		// 파라미터 검증
		validateJoinParameters(challengeId, userId);

		// 사용자 정보 조회
		String userName = getUserName(userId);

		// 시스템 메시지 생성
		String message = userName + "님이 입장했습니다.";
		ChatMessageVO systemVO = converter.createSystemMessage(challengeId, userId, userName, message);

		// VO → DTO 변환하여 반환
		ChatMessageDTO resultDTO = ChatMessageDTO.builder()
			.challengeId(systemVO.getChallengeId())
			.userId(systemVO.getUserId())
			.userName(systemVO.getUserName())
			.message(systemVO.getMessage())
			.messageType(systemVO.getMessageType())
			.sentAt(systemVO.getSentAt())
			.build();

		log.debug("입장 처리 완료: userId={}, userName={}", userId, userName);
		return resultDTO;
	}

	/**
	 * 사용자 퇴장 처리
	 */
	public ChatMessageDTO handleLeave(Long challengeId, Long userId, String userName) {
		log.debug("사용자 퇴장 처리: challengeId={}, userId={}, userName={}", challengeId, userId, userName);

		if (challengeId == null) {
			throw new IllegalArgumentException("챌린지 ID는 필수입니다.");
		}

		String displayName = (userName != null && !userName.trim().isEmpty())
			? userName
			: ("사용자" + userId);

		// 시스템 메시지 생성
		String message = displayName + "님이 퇴장했습니다.";
		ChatMessageVO systemVO = converter.createSystemMessage(challengeId, userId, displayName, message);

		// VO → DTO 변환하여 반환
		ChatMessageDTO resultDTO = ChatMessageDTO.builder()
			.challengeId(systemVO.getChallengeId())
			.userId(systemVO.getUserId())
			.userName(systemVO.getUserName())
			.message(systemVO.getMessage())
			.messageType(systemVO.getMessageType())
			.sentAt(systemVO.getSentAt())
			.build();

		log.debug("퇴장 처리 완료: userName={}", displayName);
		return resultDTO;
	}

	/**
	 * 에러 메시지 생성
	 */
	public ChatMessageDTO createErrorMessage(Long challengeId, String errorMessage) {
		log.debug("에러 메시지 생성: challengeId={}, message={}", challengeId, errorMessage);

		ChatMessageVO errorVO = converter.createErrorMessage(challengeId, errorMessage);

		return ChatMessageDTO.builder()
			.challengeId(errorVO.getChallengeId())
			.message(errorVO.getMessage())
			.messageType(errorVO.getMessageType())
			.sentAt(errorVO.getSentAt())
			.build();
	}

	/**
	 * 메시지 검증
	 */
	private void validateMessage(ChatMessageVO vo) {
		if (!vo.isValidMessage()) {
			throw new IllegalArgumentException("유효하지 않은 메시지입니다: " + vo.getMessage());
		}
	}

	/**
	 * 입장 파라미터 검증
	 */
	private void validateJoinParameters(Long challengeId, Long userId) {
		if (challengeId == null) {
			throw new IllegalArgumentException("챌린지 ID는 필수입니다.");
		}
		if (userId == null) {
			throw new IllegalArgumentException("사용자 ID는 필수입니다.");
		}
	}

	/**
	 * 사용자 이름 조회
	 */
	private String getUserName(Long userId) {
		String defaultName = "사용자" + userId;

		try {
			Map<String, Object> userMap = chatMapper.selectUserById(userId);

			if (userMap != null && !userMap.isEmpty()) {
				String dbUserName = (String)userMap.get("name");
				if (dbUserName != null && !dbUserName.trim().isEmpty()) {
					log.debug("사용자 정보 조회 성공: userId={}, name={}", userId, dbUserName);
					return dbUserName;
				}
			}

			log.warn("사용자 ID {}의 정보를 DB에서 찾을 수 없습니다. 기본값 사용: {}", userId, defaultName);
			return defaultName;

		} catch (Exception e) {
			log.error("사용자 정보 조회 중 오류 (userId: {}): {}", userId, e.getMessage());
			return defaultName;
		}
	}
}