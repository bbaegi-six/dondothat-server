package org.bbagisix.challenge.service;

import lombok.RequiredArgsConstructor;
import org.bbagisix.challenge.dto.ChallengeDTO;
import org.bbagisix.challenge.mapper.ChallengeMapper;
import org.bbagisix.challenge.domain.ChallengeVO;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChallengeService {

	private final ChallengeMapper challengeMapper;

	public ChallengeDTO getChallengeById(Long challengeId) {
		ChallengeVO vo = challengeMapper.findByChallengeId(challengeId);
		return ChallengeDTO.from(vo);
	}
}
