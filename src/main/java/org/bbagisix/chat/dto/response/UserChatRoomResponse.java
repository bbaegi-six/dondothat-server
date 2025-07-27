package org.bbagisix.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserChatRoomResponse {

	private final Long userId;
	private final Long challengeId;
	private final String challengeName;
	private final String status;
	private final String message;
}
