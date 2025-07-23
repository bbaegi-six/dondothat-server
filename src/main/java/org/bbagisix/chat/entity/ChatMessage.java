package org.bbagisix.chat.entity;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
	private Long messageId;
	private Long challengeId;
	private Long userId;
	private String message;
	private Timestamp sentAt;
	private String messageType;

	// DB 조인 결과용 (사용자 정보)
	private String userName;
}
