package org.bbagisix.chat.mapper;

import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bbagisix.chat.entity.ChatMessage;

@Mapper
public interface ChatMapper {

	// 채팅 메시지 저장
	int insertMessage(ChatMessage chatMessage);

	// 특정 메시지 조회 (사용자 정보 포함)
	ChatMessage selectMessageById(@Param("messageId") Long messageId);

	Map<String, Object> selectUserById(@Param("userId") Long userId);
}
