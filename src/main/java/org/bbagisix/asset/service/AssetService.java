package org.bbagisix.asset.service;

import org.bbagisix.asset.dto.AssetDTO;

public interface AssetService {
	public void connectAsset(Long userId, AssetDTO assetDTO);
	// 계정 삭제 -> 소비내역 삭제

	// 특정 날짜 데이터 가져오기 + db 저장
}
