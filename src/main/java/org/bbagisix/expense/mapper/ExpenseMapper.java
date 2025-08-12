package org.bbagisix.expense.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bbagisix.expense.domain.ExpenseVO;
import org.bbagisix.expense.dto.ExpenseDTO;

@Mapper
public interface ExpenseMapper {

	void insert(ExpenseVO expense);

	ExpenseVO findById(Long expenditureId);

	List<ExpenseVO> findAllByUserId(Long userId);

	List<ExpenseDTO> findAllByUserIdWithDetails(Long userId);

	int update(ExpenseVO expense);

	int delete(Long expenditureId, Long userId);

	List<ExpenseVO> getRecentExpenses(Long userId);

	List<Long> getTodayExpenseCategories(Long userId);

	Long getSumOfPeriodExpenses(@Param("userId") Long userId,
		@Param("categoryId") Long categoryId,
		@Param("period") Long period);

	int insertExpenses(List<ExpenseVO> expenses);
}
