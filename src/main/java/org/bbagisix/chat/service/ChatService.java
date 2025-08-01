package org.bbagisix.chat.service;

import java.util.List;
import java.util.Map;

import org.bbagisix.category.mapper.CategoryMapper;
import org.bbagisix.chat.converter.ChatMessageConverter;
import org.bbagisix.chat.domain.ChatMessageVO;
import org.bbagisix.chat.dto.ChatMessageDTO;
import org.bbagisix.chat.entity.ChatMessage;
import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
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
	private final ChatMessagePublisher chatMessagePublisher;
	private final CategoryMapper categoryMapper;

	/**
	 * 채팅 메시지 저장
	 * Redis 발행(pub)
	 */
	public ChatMessageDTO saveMessage(ChatMessageDTO dto) {
		log.debug("채팅 메시지 저장 요청: challengeId={}, userId={}, message={}",
			dto.getChallengeId(), dto.getUserId(), dto.getMessage());

		try {
			// 1. DTO -> VO 변환
			ChatMessageVO vo = converter.toVO(dto);

			// 2. 비즈니스 로직 검증
			validateMessage(vo);

			// 3. VO -> Entity 변환
			ChatMessage entity = converter.toEntity(vo);

			// 4. DB 저장
			int result = chatMapper.insertMessage(entity);
			if (result != 1) {
				throw new BusinessException(ErrorCode.WEBSOCKET_SEND_FAILED);
			}

			// 5. 저장된 메시지 조회 (사용자 정보 포함)
			ChatMessage savedEntity = chatMapper.selectMessageById(entity.getMessageId());
			if (savedEntity == null) {
				throw new BusinessException(ErrorCode.MESSAGE_LOAD_FAILED);
			}

			// 6. Entity -> DTO 변환하여 반환
			ChatMessageDTO resultDTO = converter.toDTO(savedEntity);

			// 7. Redis pub/sub로 메시지 발행 (새로 추가된 부분)
			try {
				chatMessagePublisher.publishMessage(dto.getChallengeId(), resultDTO);
				log.debug("메시지 Redis 발행 완료: messageId={}", resultDTO.getMessageId());
			} catch (Exception e) {
				log.error("Redis 메시지 발행 실패 (DB 저장은 성공): messageId={}", resultDTO.getMessageId(), e);
			}
			log.debug("메시지 저장 완료: messageId={}", resultDTO.getMessageId());
			return resultDTO;

		} catch (BusinessException e) {
			log.warn("비즈니스 예외 발생: code={}, message={}", e.getCode(), e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("메시지 저장 중 예상하지 못한 오류: ", e);
			throw new BusinessException(ErrorCode.DATA_ACCESS_ERROR, e);
		}
	}

	/**
	 * 사용자 입장 처리
	 */
	public ChatMessageDTO handleJoin(Long challengeId, Long userId) {
		log.debug("사용자 입장 처리: challengeId={}, userId={}", challengeId, userId);

		// 파라미터 검증
		validateJoinParameters(challengeId, userId);

		try {
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

			// Redis pub/sub로 입장 메시지 발행
			try {
				chatMessagePublisher.publishMessage(challengeId, resultDTO);
				log.debug("입장 메시지 Redis 발행 완료: userId={}, userName={}", userId, userName);
			} catch (Exception e) {
				log.error("입장 메시지 Redis 발행 실패: userId={}, userName={}", userId, userName, e);
				// 입장 메시지 발행 실패해도 입장 처리는 계속 진행
			}

			log.debug("입장 처리 완료: userId={}, userName={}", userId, userName);
			return resultDTO;
		} catch (Exception e) {
			log.error("입장 처리 중 오류: ", e);
			throw new BusinessException(ErrorCode.DATA_ACCESS_ERROR, "입장 처리 중 오류가 발생했습니다.", e);
		}
	}

	/**
	 * 사용자 퇴장 처리
	 */
	public ChatMessageDTO handleLeave(Long challengeId, Long userId, String userName) {
		log.debug("사용자 퇴장 처리: challengeId={}, userId={}, userName={}", challengeId, userId, userName);

		if (challengeId == null) {
			throw new BusinessException(ErrorCode.CHALLENGE_ID_REQUIRED);
		}

		try {
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

			// Redis pub/sub로 퇴장 메시지 발행
			try {
				chatMessagePublisher.publishMessage(challengeId, resultDTO);
				log.debug("퇴장 메시지 Redis 발행 완료: userName={}", displayName);
			} catch (Exception e) {
				log.error("퇴장 메시지 Redis 발행 실패: userName={}", displayName, e);
				// 퇴장 메시지 발행 실패해도 퇴장 처리는 계속 진행
			}
			log.debug("퇴장 처리 완료: userName={}", displayName);
			return resultDTO;
		} catch (Exception e) {
			log.error("퇴장 처리 중 오류: ", e);
			throw new BusinessException(ErrorCode.DATA_ACCESS_ERROR, "퇴장 처리 중 오류가 발생했습니다.", e);
		}
	}

	/**
	 * 사용자가 참여중인 채팅방 목록 조회
	 */
	public Map<String, Object> getUserCurrentChatRoom(Long userId) {
		log.debug("사용자 채팅 목록 조회: userId={}", userId);

		if (userId == null) {
			throw new BusinessException(ErrorCode.USER_ID_REQUIRED);
		}

		try {
			Map<String, Object> currentChatRoom = chatMapper.selectUserCurrentChatRoom(userId);

			if (currentChatRoom == null || currentChatRoom.isEmpty()) {
				// 참여중인 챌린지가 없는 경우
				log.debug("사용자 {}가 참여 중인 챌린지가 없습니다.", userId);
				return Map.of(
					"userId", userId,
					"challengeId", null,
					"challengeName", null,
					"status", "no_challenge",
					"message", "참여 중인 챌린지가 없습니다."
				);
			}
			// 참여중인 챌린지가 있는 경우
			return currentChatRoom;
		} catch (Exception e) {
			log.error("채팅방 목록 조회 중 오류: ", e);
			throw new BusinessException(ErrorCode.DATA_ACCESS_ERROR, "채팅방 목록을 불러올 수 없습니다.");
		}
	}

	/**
	 * 특정 채팅방의 참여자 목록 조회
	 */
	public List<Map<String, Object>> getParticipants(Long challengeId) {
		log.debug("참여자 목록 조회: challengeId={}", challengeId);

		if (challengeId == null) {
			throw new BusinessException(ErrorCode.CHALLENGE_ID_REQUIRED);
		}

		try {
			return chatMapper.selectParticipants(challengeId);
		} catch (Exception e) {
			log.error("참여자 목록 조회중 오류: ", e);
			throw new BusinessException(ErrorCode.DATA_ACCESS_ERROR, "참여자 목록을 불러올 수 없습니다.", e);
		}
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
		if (vo.getMessage() == null || vo.getMessage().trim().isEmpty()) {
			throw new BusinessException(ErrorCode.MESSAGE_EMPTY);
		}

		if (vo.getMessage().length() > 255) {
			throw new BusinessException(ErrorCode.MESSAGE_TOO_LONG);
		}

		if (vo.getMessage().contains("<") || vo.getMessage().contains(">")) {
			throw new BusinessException(ErrorCode.MESSAGE_CONTAINS_HTML);
		}

		if (!vo.isValidMessage()) {
			throw new BusinessException(ErrorCode.INVALID_MESSAGE);
		}
	}

	/**
	 * 입장 파라미터 검증
	 */
	private void validateJoinParameters(Long challengeId, Long userId) {
		if (challengeId == null) {
			throw new BusinessException(ErrorCode.CHALLENGE_ID_REQUIRED);
		}
		if (userId == null) {
			throw new BusinessException(ErrorCode.USER_ID_REQUIRED);
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