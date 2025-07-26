package org.bbagisix.challenge.service;

import lombok.RequiredArgsConstructor;
import org.bbagisix.challenge.domain.ChallengeVO;
import org.bbagisix.challenge.dto.ChallengeDTO;
import org.bbagisix.challenge.mapper.ChallengeMapper;
import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChallengeService {

	private final ChallengeMapper challengeMapper;

	public ChallengeDTO getChallengeById(Long challengeId) {
		if (challengeId == null) {
			throw new BusinessException(ErrorCode.CHALLENGE_ID_REQUIRED);
		}

		ChallengeVO vo = challengeMapper.findByChallengeId(challengeId);
		if (vo == null) {
			throw new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND);
		}

		return ChallengeDTO.from(vo);
	}
}
