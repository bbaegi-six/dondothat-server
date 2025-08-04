package org.bbagisix.chat.dto;

import java.time.LocalDateTime;

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
public class ChatHistoryDTO {

	private Long messageId;
	private Long challengeId;
	private Long userId;
	private String message;
	private LocalDateTime sentAt;
	private String messageType;
	private String userName;

	// 추가 정보 (필요시)
	// private final Boolean isMyMessage;
}
