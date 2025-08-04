package org.bbagisix.chat.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bbagisix.chat.dto.ChatHistoryDTO;
import org.bbagisix.chat.dto.UserChallengeInfoDTO;
import org.bbagisix.chat.entity.ChatMessage;

@Mapper
public interface ChatMapper {

	// 채팅 메시지 저장 (Entity)
	int insertMessage(ChatMessage chatMessage);

	// 특정 메시지 조회 (사용자 정보 포함)
	ChatMessage selectMessageById(@Param("messageId") Long messageId);

	// 사용자 정보 조회
	Map<String, Object> selectUserById(@Param("userId") Long userId);

	// 사용자가 현재 참여중인 챌린지 채팅방 조회 (단일)
	Map<String, Object> selectUserCurrentChatRoom(@Param("userId") Long userId);

	// 특정 챌린지의 참여자 목록 조회
	List<Map<String, Object>> selectParticipants(@Param("challengeId") Long challengeId);

	// 사용자가 챌린지에 참여한 시점 이후의 채팅 메시지 조회
	List<ChatHistoryDTO> selectChatHistoryByUserParticipation(
		@Param("challengeId") Long challengeId,
		@Param("userId") Long userId,
		@Param("limit") int limit
	);

	// 사용자가 해당 챌린지에 현재 참여 중인지 확인
	UserChallengeInfoDTO selectUserChallengeStatus(
		@Param("challengeId") Long challengeId,
		@Param("userId") Long userId
	);

	// 사용자의 현재 활성 챌린지 정보 조회
	UserChallengeInfoDTO selectUserActiveChallengeInfo(@Param("userId") Long userId);
}
