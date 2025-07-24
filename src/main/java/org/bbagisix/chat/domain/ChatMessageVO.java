package org.bbagisix.chat.domain;

import java.sql.Timestamp;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMessageVO {
	private final Long challengeId;
	private final Long userId;
	private final String message;
	private final String messageType;
	private final Timestamp sentAt;
	private final String userName;

	// 비즈니스 로직 메서드
	public boolean isSystemMessage() {
		return "SYSTEM".equals(messageType);
	}

	public boolean isErrorMessage() {
		return "ERROR".equals(messageType);
	}

	public boolean isValidMessage() {
		if (message == null || message.trim().isEmpty()) {
			return false;
		}

		// 길이 제한 (255자)
		if (message.length() > 255) {
			return false;
		}

		// HTML 태그 포함 여부 검사
		if (message.contains("<") || message.contains(">")) {
			return false;
		}

		return true;
	}

	public String getDisplayMessage() {
		if (isSystemMessage()) {
			return "🔔 " + message;
		} else if (isErrorMessage()) {
			return "❌ " + message;
		}

		String displayName = userName != null ? userName : ("사용자" + userId);
		return "[" + displayName + "]" + message;
	}

	public String getMessageTypeForDisplay() {
		return messageType != null ? messageType : "MESSAGE";
	}
}
