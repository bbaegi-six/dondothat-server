package org.bbagisix.challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bbagisix.challenge.domain.ChallengeVO;

@Getter
@AllArgsConstructor
public class ChallengeDTO {

	private Long challengeId;
	private Long categoryId;
	private String title;
	private String summary;
	private String description;

	public static ChallengeDTO from(ChallengeVO vo) {
		return new ChallengeDTO(
			vo.getChallengeId(),
			vo.getCategoryId(),
			vo.getTitle(),
			vo.getSummary(),
			vo.getDescription()
		);
	}
}
