package org.bbagisix.expense.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bbagisix.expense.domain.ExpenseVO;
import org.bbagisix.expense.dto.ExpenseDTO;

@Mapper
public interface ExpenseMapper {

	void insert(ExpenseVO expense);

	ExpenseVO findById(Long expenditureId);

	List<ExpenseDTO> findAllByUserIdWithDetails(Long userId);

	int update(ExpenseVO expense);

	List<ExpenseVO> getRecentExpenses(Long userId);

	List<Long> getTodayExpenseCategories(Long userId);

	Long getSumOfPeriodExpenses(@Param("userId") Long userId,
		@Param("categoryId") Long categoryId,
		@Param("period") Long period);

	int insertExpenses(List<ExpenseVO> expenses);

	// codef 거래 ID로 중복 개수 확인
	int countByCodefTransactionId(String codefTransactionId);

	// codef 거래 ID로 실제 거래 데이터 조회
	List<ExpenseVO> findByCodefTransactionId(String codefTransactionId);

	// 내역을 물리적 삭제 대신 소프트 삭제
	int softDelete(Long expenditureId, Long userId);

	// 현재월 카테고리별 지출 집계
	Map<String, Long> getCurrentMonthSummaryByCategory(Long userId);
}
