package org.bbagisix.saving.service;

import java.util.List;
import java.util.stream.Collectors;

import org.bbagisix.asset.domain.AssetVO;
import org.bbagisix.asset.mapper.AssetMapper;
import org.bbagisix.challenge.domain.ChallengeVO;
import org.bbagisix.challenge.domain.UserChallengeVO;
import org.bbagisix.challenge.mapper.ChallengeMapper;
import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
import org.bbagisix.saving.dto.SavingDTO;
import org.bbagisix.saving.mapper.SavingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class SavingService {

	private final SavingMapper savingMapper;
	private final ChallengeMapper challengeMapper;
	private final AssetMapper assetMapper;

	public List<SavingDTO> getSavingHistory(Long userId) {
		try {
			return savingMapper.getSavingHistory(userId).stream().map(this::voToDto).collect(Collectors.toList());
		} catch (Exception e) {
			log.error("사용자 저금 내역 조회 중 오류 발생: userId={}, error={}", userId, e.getMessage(), e);
			throw new BusinessException(ErrorCode.DATA_ACCESS_ERROR, e);
		}
	}

	public Long getTotalSaving(Long userId) {
		try {
			return savingMapper.getTotalSaving(userId);
		} catch (Exception e) {
			log.error("사용자 총 저금액 조회 중 오류 발생: userId={}, error={}", userId, e.getMessage(), e);
			throw new BusinessException(ErrorCode.DATA_ACCESS_ERROR, e);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void updateSaving(Long userId, Long userChallengeId) {

		try {// 해당 userChallenge 조회
			UserChallengeVO userChallenge;
			userChallenge = challengeMapper.getUserChallengeById(userChallengeId);
			if (userChallenge == null)
				throw new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND);
			if (!userChallenge.getUserId().equals(userId))
				throw new BusinessException(ErrorCode.SAVING_UPDATE_DENIED);
			Long totalSaving = userChallenge.getSaving() * userChallenge.getPeriod();

			// user 계좌(sub) 조회
			AssetVO asset;
			asset = assetMapper.selectAssetByUserIdAndStatus(userId, "sub");
			if (asset == null)
				throw new BusinessException(ErrorCode.ASSET_NOT_FOUND);

			// balance update
			int updated = assetMapper.updateSavingAssetBalance(asset.getAssetId(), totalSaving);
			if (updated != 1) {
				throw new BusinessException(ErrorCode.SAVING_UPDATE_FAILED);
			}

		} catch (BusinessException be) {
			throw be;
		} catch (Exception e) {
			log.error("asset(저금통) balance 업데이트 중 알 수 없는 오류 발생: error={}", e.getMessage(), e);
			throw new BusinessException(ErrorCode.DATA_ACCESS_ERROR, e);
		}
	}

	private SavingDTO voToDto(UserChallengeVO userChallengeVO) {
		if (userChallengeVO == null)
			return null;
		ChallengeVO challengeVO = challengeMapper.findByChallengeId(userChallengeVO.getChallengeId());
		return SavingDTO.builder()
			.categoryId(challengeVO.getCategoryId())
			.title(challengeVO.getTitle())
			.startDate(userChallengeVO.getStartDate())
			.endDate(userChallengeVO.getEndDate())
			.saving(userChallengeVO.getSaving())
			.build();
	}

}
