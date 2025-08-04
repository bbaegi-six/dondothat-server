package org.bbagisix.chat.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserChallengeStatusResponse {

	private final Long userId;
	private final Long challengeId;
	private final String challengeName;
	private final Boolean hasActiveChallenge;
	private final String status;
	private final String message;
	private final LocalDateTime startDate;
	private final LocalDateTime endDate;
}
