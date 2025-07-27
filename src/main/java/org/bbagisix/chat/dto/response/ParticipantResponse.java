package org.bbagisix.chat.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ParticipantResponse {

	private final Long userId;
	private final String userName;
	private final LocalDateTime joinedAt;
	private final Boolean isActive;
}
