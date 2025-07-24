package org.bbagisix.chat.dto;

import java.sql.Timestamp;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

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

	@NotNull(message = "챌린지 ID는 필수입니다")
	private Long challengeId;    // challenge_id (BIGINT)

	@NotNull(message = "사용자 ID는 필수입니다")
	private Long userId;         // user_id (BIGINT)

	@NotNull(message = "메시지는 필수입니다")
	@Size(min = 1, max = 255, message = "메시지는 1자 이상 255자 이하여야 합니다")
	private String message;      // message (VARCHAR)

	private Timestamp sentAt;    // sent_at (DATETIME)
	private String messageType;  // message_type (VARCHAR)

	// 추가 필드 (Join)
	private String userName;

	// 클라이언트 전송용 생성자
	public ChatMessageDTO(Long challengeId, Long userId, String message, String messageType) {
		this.challengeId = challengeId;
		this.userId = userId;
		this.message = message;
		this.messageType = messageType;
		this.sentAt = new Timestamp(System.currentTimeMillis());
	}
}
