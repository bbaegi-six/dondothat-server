package org.bbagisix.chat.dto;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {

	private Long messageId;     // message_id (BIGINT)
	private Long challengeId;    // challenge_id (BIGINT)
	private Long userId;         // user_id (BIGINT)
	private String message;      // message (VARCHAR)
	private Timestamp sentAt;    // sent_at (DATETIME)
	private String messageType;  // message_type (VARCHAR)

	// 추가 필드 (Join)
	private String userName;
	private String userProfileImage;

	// 클라이언트 전송용 생성자
	public ChatMessageDTO(Long challengeId, Long userId, String message, String messageType) {
		this.challengeId = challengeId;
		this.userId = userId;
		this.message = message;
		this.messageType = messageType;
		this.sentAt = new Timestamp(System.currentTimeMillis());
	}
}
