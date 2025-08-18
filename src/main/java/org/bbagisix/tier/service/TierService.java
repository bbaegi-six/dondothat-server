package org.bbagisix.tier.service;

import java.util.List;
import java.util.stream.Collectors;

import org.bbagisix.common.exception.BusinessException;
import org.bbagisix.common.exception.ErrorCode;
import org.bbagisix.tier.domain.TierVO;
import org.bbagisix.tier.dto.TierDTO;
import org.bbagisix.tier.mapper.TierMapper;
import org.bbagisix.user.domain.UserVO;
import org.bbagisix.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TierService {

	private final TierMapper tierMapper;
	private final UserMapper userMapper;

	// 모든 티어 목록 조회
	public List<TierDTO> getAllTiers() {
		List<TierVO> allTiers = tierMapper.findAllTiers();

		return allTiers.stream()
			.map(TierDTO::from)
			.collect(Collectors.toList());
	}

	// 사용자의 현재 티어 조회
	public TierDTO getUserCurrentTier(Long userId) {
		if (userId == null) {
			throw new BusinessException(ErrorCode.USER_ID_REQUIRED);
		}

		// 사용자 정보 조회
		UserVO user = userMapper.findByUserId(userId);
		if (user == null) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}

		// tier_id가 null이면 아직 티어가 없는 상태
		if (user.getTierId() == null) {
			return null;    // 또는 기본 티어 반환
		}

		// 현재 티어 조회
		TierVO currentTier = tierMapper.findById(user.getTierId());
		if (currentTier == null) {
			log.warn("사용자 {}의 tier_id {}에 해당하는 티어를 찾을 수 없습니다.", userId, user.getTierId());
			return null;
		}

		return TierDTO.from(currentTier);
	}

	// 챌린지 성공 시 티어 승급 처리
	@Transactional
	public void promoteUserTier(Long userId) {
		// 완료한 챌린지 수 조회
		Integer completedChallenges = userMapper.getCompletedChallengeCount(userId);

		// 완료한 챌린지 수에 기반한 적절한 tier 계산
		Long appropriateTierId = calculateTierByCompletedChallenges(completedChallenges);

		// 현재 사용자 정보 조회
		UserVO user = userMapper.findByUserId(userId);
		Long currentTierId = user.getTierId();

		// 계산된 tier가 현재 tier보다 높은 경우에만 업데이트
		if (appropriateTierId > (currentTierId != null ? currentTierId : 0L)) {
			// 해당 tier가 실제로 존재하는지 확인
			TierVO targetTier = tierMapper.findById(appropriateTierId);
			if (targetTier != null) {
				int updateResult = userMapper.updateUserTier(userId, appropriateTierId);
				if (updateResult > 0) {
					log.info("✅ 사용자 {} 티어 승급 성공: {} → {} (완료한 챌린지: {}개)",
						userId, currentTierId, appropriateTierId, completedChallenges);
				} else {
					log.error("❌ 사용자 {} 티어 업데이트 실패", userId);
				}
			} else {
				log.error("❌ tier_id {} 에 해당하는 tier가 존재하지 않음", appropriateTierId);
			}
		} else {
			log.info("⏭️ tier 업데이트 조건 불만족 - 현재: {}, 계산된: {}", currentTierId, appropriateTierId);
		}
	}

	// 완료한 챌린지 수에 따른 tier 계산
	private Long calculateTierByCompletedChallenges(Integer completedChallenges) {
		if (completedChallenges == null || completedChallenges == 0) {
			return 1L; // default
		} else if (completedChallenges <= 1) {
			return 1L; // 브론즈
		} else if (completedChallenges <= 2) {
			return 2L; // 실버
		} else if (completedChallenges <= 3) {
			return 3L; // 골드
		} else if (completedChallenges <= 4) {
			return 4L; // 플래티넘
		} else if (completedChallenges <= 5) {
			return 5L; // 루비
		} else {
			return 6L; // 에메랄드
		}
	}
}
