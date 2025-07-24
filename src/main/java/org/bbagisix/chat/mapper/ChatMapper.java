package org.bbagisix.chat.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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
}
