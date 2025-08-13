package org.bbagisix.challenge.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bbagisix.analytics.service.AnalyticsService;
import org.bbagisix.challenge.domain.ChallengeVO;
import org.bbagisix.challenge.domain.UserChallengeVO;
import org.bbagisix.challenge.dto.ChallengeDTO;
import org.bbagisix.challenge.dto.ChallengeProgressDTO;
import org.bbagisix.challenge.mapper.ChallengeMapper;
import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
import org.bbagisix.expense.mapper.ExpenseMapper;
import org.bbagisix.tier.service.TierService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChallengeService {

	private final ChallengeMapper challengeMapper;
	private final ExpenseMapper expenseMapper;
	private final AnalyticsService analyticsService;
	private final TierService tierService;

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

	public ChallengeProgressDTO getChallengeProgress(Long userId) {
		if (userId == null) {
			throw new BusinessException(ErrorCode.USER_ID_REQUIRED);
		}

		// 사용자 존재 여부 확인
		if (!challengeMapper.existsUser(userId)) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}

		return challengeMapper.getChallengeProgress(userId);
	}

	public void joinChallenge(Long userId, Long challengeId, Long period) {

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

		// saving 계산
		Long categoryId = challengeMapper.getCategoryByChallengeId(challengeId);
		Long total = Optional.ofNullable(
			expenseMapper.getSumOfPeriodExpenses(userId, categoryId, period)
		).orElse(0L);
		Long saving = total / period;

		// 챌린지 참여
		UserChallengeVO userChallenge = UserChallengeVO.builder()
			.userId(userId)
			.challengeId(challengeId)
			.period(period)
			.saving(saving)
			.build();
		challengeMapper.joinChallenge(userChallenge);
	}

	public void closeChallenge(Long userChallengeId) {
		UserChallengeVO userChallenge = challengeMapper.getUserChallengeById(userChallengeId);
		if (userChallenge == null) {
			throw new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND);
		}
		UserChallengeVO updated = userChallenge.toBuilder().status("closed").build();
		challengeMapper.updateChallenge(updated);
	}

	// 챌린지 성공/실패 판단 및 진척도 계산
	@Transactional
	public void dailyCheck() {

		List<UserChallengeVO> challenges = challengeMapper.getOngoingChallenges(); // ongoing인 챌린지 조회

		for (UserChallengeVO c : challenges) {
			Long challengeCtg = challengeMapper.getCategoryByChallengeId(c.getChallengeId()); // 해당 챌린지의 카테고리 아이디 조회
			Long userId = c.getUserId();
			List<Long> expenseCtg
				= expenseMapper.getTodayExpenseCategories(userId); // 유저 소비내역의 카테고리 아이디 조회

			UserChallengeVO updated;

			if (expenseCtg.contains(challengeCtg)) { // 소비내역에 현재 챌린지의 카테고리가 포함된 경우 -> 실패
				updated = c.toBuilder()
					.status("failed")
					.endDate(new Date())
					.build();
			} else { // 유저 소비내역에 현재 챌린지의 카테고리가 포함되지 않은 경우 -> 하루 성공
				boolean isLastDay = c.getEndDate().toInstant().atZone(ZoneId.systemDefault())
					.toLocalDate().equals(LocalDate.now());

				updated = c.toBuilder()
					.progress(c.getProgress() + 1)
					.status(isLastDay ? "completed" : c.getStatus()) // 마지막 날인 경우 -> 최종 성공
					.build();
				
				// 챌린지 완료 시 tier 승급 처리
				if (isLastDay) {
					tierService.promoteUserTier(userId);
				}
			}

			challengeMapper.updateChallenge(updated);
		}
	}
}