package org.bbagisix.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ParticipantCountResponse {

	private final Long challengeId;
	private final Integer participantCount;
}
