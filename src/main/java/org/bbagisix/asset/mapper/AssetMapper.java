package org.bbagisix.asset.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.bbagisix.asset.domain.AssetVO;
import org.bbagisix.expense.domain.ExpenseVO;

import java.util.List;

@Mapper
public interface AssetMapper {
	void insertUserAsset(AssetVO assetVO);

	AssetVO selectAssetByUserId(Long userId);

	int deleteUserAssetByUserId(Long userId);

	int deleteExpensesByUserId(Long userId);
	int insertExpenses(List<ExpenseVO> expenseVO);
}
