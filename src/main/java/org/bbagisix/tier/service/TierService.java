package org.bbagisix.tier.service;

import java.util.List;
import java.util.stream.Collectors;

import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
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
		UserVO user = userMapper.findByUserId(userId);
		Long nextTierId = (user.getTierId() == null) ? 1L : user.getTierId() + 1;
		TierVO nextTier = tierMapper.findById(nextTierId);

		if (nextTier != null) {
			userMapper.updateUserTier(userId, nextTierId);
			log.info("사용자 {} 티어 승급: {} → {}", userId, user.getTierId(), nextTierId);
		}
	}
}
