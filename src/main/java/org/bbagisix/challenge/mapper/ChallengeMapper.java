package org.bbagisix.challenge.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bbagisix.challenge.domain.ChallengeVO;
import org.bbagisix.challenge.domain.UserChallengeVO;
import org.bbagisix.challenge.dto.ChallengeProgressDTO;

@Mapper
public interface ChallengeMapper {
	ChallengeVO findByChallengeId(@Param("challengeId") Long challengeId);

	boolean hasActiveChallenge(@Param("userId") Long userId);

	boolean existsUser(@Param("userId") Long userId);

	boolean existsUserChallenge(@Param("challengeId") Long challengeId, @Param("userId") Long userId);

	void joinChallenge(UserChallengeVO userChallenge);

	// 추천 카테고리에 해당하는 챌린지들 조회
	List<ChallengeVO> findChallengesByCategoryIds(@Param("categoryIds") List<Long> categoryIds,
		@Param("userId") Long userId);

	// 사용자 챌린지 진척도 조회
	ChallengeProgressDTO getChallengeProgress(@Param("userId") Long userId);

	List<UserChallengeVO> getOngoingChallenges();

	Long getCategoryByChallengeId(Long challengeId);

	void updateChallenge(UserChallengeVO userChallenge);

	UserChallengeVO getUserChallengeById(Long userChallengeId);

	// 완료된 챌린지들만 조회 (completed + failed)
	List<UserChallengeVO> getUserCompletedChallenges(@Param("userId") Long userId);


}