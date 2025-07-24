package org.bbagisix.challenge.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bbagisix.challenge.domain.ChallengeVO;

@Mapper
public interface ChallengeMapper {
	ChallengeVO findByChallengeId(@Param("challengeId") Long challengeId);
	boolean existsUserChallenge(@Param("challengeId") Long challengeId, @Param("userId") Long userId);
}
