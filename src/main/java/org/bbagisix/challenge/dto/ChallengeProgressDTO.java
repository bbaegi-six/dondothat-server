package org.bbagisix.challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChallengeProgressDTO {
	private Long user_challenge_id;
	private Long challenge_id;
	private String title;
	private String status;
	private Long period;
	private Long progress;
	private Long saving;
}