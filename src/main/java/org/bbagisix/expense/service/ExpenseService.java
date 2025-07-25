package org.bbagisix.expense.service;

import java.util.List;

import org.bbagisix.expense.domain.ExpenseVO;
import org.bbagisix.expense.dto.ExpenseDTO;

public interface ExpenseService {
	ExpenseDTO createExpense(ExpenseDTO expenseDTO);

	ExpenseDTO getExpenseById(Long expenditureId);

	List<ExpenseDTO> getExpensesByUserId(Long userId);

	ExpenseDTO updateExpense(Long expenditureId, ExpenseDTO expenseDTO);

	void deleteExpense(Long expenditureId);

	List<ExpenseVO> getRecentExpenses(Long userId);

	ExpenseVO updateExpenseCategory(Long expenditureId, Long categoryId);
}
