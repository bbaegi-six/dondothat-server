package org.bbagisix.mypage.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bbagisix.asset.domain.AssetVO;
import org.bbagisix.asset.mapper.AssetMapper;
import org.bbagisix.mypage.domain.MyPageAccountDTO;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class MyPageService {

	private final AssetMapper assetMapper;

	public Map<String, Object> getUserAccountData(Long userId) {
		Map<String, Object> result = new HashMap<>();

		// main 계좌 조회
		AssetVO mainAsset = assetMapper.selectAssetByUserIdAndStatus(userId, "main");
		MyPageAccountDTO mainAccount = mainAsset != null ? convertToDTO(mainAsset) : null;

		// sub 계좌 조회
		AssetVO subAsset = assetMapper.selectAssetByUserIdAndStatus(userId, "sub");
		MyPageAccountDTO subAccount = subAsset != null ? convertToDTO(subAsset) : null;

		result.put("mainAccount", mainAccount);
		result.put("subAccount", subAccount);

		return result;
	}

	private MyPageAccountDTO convertToDTO(AssetVO assetVO) {
		return MyPageAccountDTO.builder()
			.assetId(assetVO.getAssetId())
			.assetName(assetVO.getAssetName())
			.bankName(assetVO.getBankName())
			.balance(assetVO.getBalance())
			.status(assetVO.getStatus())
			.build();
	}
}
