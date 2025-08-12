package org.bbagisix.mypage.service;

import org.bbagisix.asset.domain.AssetVO;
import org.bbagisix.asset.mapper.AssetMapper;
import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
import org.bbagisix.mypage.domain.MyPageAccountDTO;
import org.bbagisix.user.dto.CustomOAuth2User;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class MyPageService {

	private final AssetMapper assetMapper;

	public MyPageAccountDTO getUserAccountData(Authentication authentication) {
		Long userId = extractUserId(authentication);

		// main 계좌 조회
		AssetVO mainAsset = assetMapper.selectAssetByUserIdAndStatus(userId, "main");

		// sub 계좌 조회
		AssetVO subAsset = assetMapper.selectAssetByUserIdAndStatus(userId, "sub");

		return MyPageAccountDTO.builder()
			.mainAccount(mainAsset != null ? convertToAccountInfo(mainAsset) : null)
			.subAccount(subAsset != null ? convertToAccountInfo(subAsset) : null)
			.build();
	}

	// Authentication에서 사용자 ID 추출
	private Long extractUserId(Authentication authentication) {
		if (authentication == null || authentication.getPrincipal() == null) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
		}

		CustomOAuth2User curUser = (CustomOAuth2User)authentication.getPrincipal();
		if (curUser == null || curUser.getUserId() == null) {
			throw new BusinessException(ErrorCode.USER_ID_REQUIRED);
		}

		return curUser.getUserId();
	}

	private MyPageAccountDTO.AccountInfo convertToAccountInfo(AssetVO assetVO) {
		return MyPageAccountDTO.AccountInfo.builder()
			.assetId(assetVO.getAssetId())
			.assetName(assetVO.getAssetName())
			.bankName(assetVO.getBankName())
			.balance(assetVO.getBalance())
			.status(assetVO.getStatus())
			.build();
	}
}