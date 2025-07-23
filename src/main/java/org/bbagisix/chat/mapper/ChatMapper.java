package org.bbagisix.chat.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bbagisix.chat.dto.ChatMessageDTO;

@Mapper
public interface ChatMapper {

	// 채팅 메시지 저장
	int insertMessage(ChatMessageDTO chatMessage);

	// 특정 메시지 조회 (사용자 정보 포함)
	ChatMessageDTO selectMessageById(@Param("messageId") Long messageId);
}
