package org.bbagisix.codef.service;

import org.bbagisix.asset.dto.AssetDTO;
import org.bbagisix.codef.dto.CodefTransactionResDTO;

public interface CodefApiService {
	// connectedId 조회
	public String getConnectedId(AssetDTO assetDTO);

	// 거래 내역 조회
	public CodefTransactionResDTO getTransactionList(AssetDTO assetDTO, String connectedId, String startDate, String endDate);
}
