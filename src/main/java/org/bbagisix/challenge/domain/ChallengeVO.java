package org.bbagisix.challenge.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChallengeVO {

	private Long challengeId;
	private Long categoryId;
	private String title;
	private String summary;
	private String description;
}
