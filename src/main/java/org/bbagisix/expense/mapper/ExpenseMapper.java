package org.bbagisix.expense.mapper;

import java.util.List;

import org.bbagisix.expense.domain.ExpenseVO;

public interface ExpenseMapper {
	void insert(ExpenseVO expense);

	ExpenseVO findById(Long expenditureId);

	List<ExpenseVO> findAllByUserId(Long userId);

	int update(ExpenseVO expense);

	int delete(Long expenditureId);

	List<ExpenseVO> getRecentExpenses(Long userId);
}
