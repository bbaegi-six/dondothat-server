package org.bbagisix.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatRoomInfoResponse {

	private final Long challengeId;
	private final String challengeName;
	private final Integer participantCount;
	private final String status;
}
