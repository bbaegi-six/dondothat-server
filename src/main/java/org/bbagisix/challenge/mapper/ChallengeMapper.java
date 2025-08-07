package org.bbagisix.challenge.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bbagisix.challenge.domain.ChallengeVO;

@Mapper
public interface ChallengeMapper {
	ChallengeVO findByChallengeId(@Param("challengeId") Long challengeId);
	boolean hasActiveChallenge(@Param("userId") Long userId);
	boolean existsUser(@Param("userId") Long userId);
	boolean existsUserChallenge(@Param("challengeId") Long challengeId, @Param("userId") Long userId);
	void joinChallenge(@Param("challengeId") Long challengeId, @Param("userId") Long userId);
	void leaveChallenge(@Param("challengeId") Long challengeId, @Param("userId") Long userId);
	List<ChallengeVO> findChallengesByCategoryIds(@Param("categoryIds") List<Long> categoryIds, @Param("userId") Long userId);
}