package org.bbagisix.asset.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bbagisix.asset.domain.AssetVO;
import org.bbagisix.expense.domain.ExpenseVO;

import java.util.Date;
import java.util.List;

@Mapper
public interface AssetMapper {
	void insertUserAsset(AssetVO assetVO);

	AssetVO selectAssetByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

	int deleteUserAssetByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

	int deleteExpensesByUserId(Long userId);

	// 모든 main 계좌 조회
	List<AssetVO> selectAllMainAssets();

	// 계좌 잔액 업데이트
	void updateAssetBalance(@Param("assetId") Long assetId, @Param("newBalance") Long newBalance);

	// 중복 거래내역 개수 조회
	int countDuplicateTransaction(
		@Param("userId") Long userId,
		@Param("assetId") Long assetId,
		@Param("amount") Long amount,
		@Param("description") String description,
		@Param("expenditureDate") Date expenditureDate
	);
}
