package org.bbagisix.challenge.service;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.bbagisix.analytics.service.AnalyticsService;
import org.bbagisix.category.dto.CategoryDTO;
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
	private final AnalyticsService analyticsService;

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

	public List<ChallengeDTO> getRecommendedChallenges(Long userId) {
		if (userId == null) {
			throw new BusinessException(ErrorCode.USER_ID_REQUIRED);
		}

		// 사용자 존재 여부 확인
		if (!challengeMapper.existsUser(userId)) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}

		// AnalyticsService에서 LLM 기반 추천 카테고리 3개 조회
		List<Long> categoryIds = analyticsService.getTopCategories(userId);

		// 추천 카테고리에 해당하는 챌린지들 조회
		List<ChallengeVO> recommendedChallenges = challengeMapper.findChallengesByCategoryIds(categoryIds, userId);

		// VO를 DTO로 변환하여 반환
		return recommendedChallenges.stream()
			.map(ChallengeDTO::from)
			.collect(Collectors.toList());
	}

	public void joinChallenge(Long challengeId, Long userId) {
		if (challengeId == null) {
			throw new BusinessException(ErrorCode.CHALLENGE_ID_REQUIRED);
		}

		if (userId == null) {
			throw new BusinessException(ErrorCode.USER_ID_REQUIRED);
		}

		// 사용자 존재 여부 확인
		if (!challengeMapper.existsUser(userId)) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}

		// 챌린지 존재 여부 확인
		ChallengeVO challenge = challengeMapper.findByChallengeId(challengeId);
		if (challenge == null) {
			throw new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND);
		}

		// 해당 사용자가 어떤 챌린지든 참여 중인지 확인
		if (challengeMapper.hasActiveChallenge(userId)) {
			throw new BusinessException(ErrorCode.ALREADY_JOINED_CHALLENGE);
		}

		// 챌린지 참여
		challengeMapper.joinChallenge(challengeId, userId);
	}

	public void leaveChallenge(Long challengeId, Long userId) {
		if (challengeId == null) {
			throw new BusinessException(ErrorCode.CHALLENGE_ID_REQUIRED);
		}

		if (userId == null) {
			throw new BusinessException(ErrorCode.USER_ID_REQUIRED);
		}

		// 사용자 존재 여부 확인
		if (!challengeMapper.existsUser(userId)) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}

		// 챌린지 존재 여부 확인
		ChallengeVO challenge = challengeMapper.findByChallengeId(challengeId);
		if (challenge == null) {
			throw new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND);
		}

		// 해당 챌린지에 실제로 참여하고 있는지 확인
		if (!challengeMapper.existsUserChallenge(challengeId, userId)) {
			throw new BusinessException(ErrorCode.CHALLENGE_NOT_JOINED);
		}

		// 챌린지 탈퇴
		challengeMapper.leaveChallenge(challengeId, userId);
	}
}