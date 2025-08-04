package org.bbagisix.expense.service;

import java.util.List;

import org.bbagisix.expense.domain.ExpenseVO;
import org.bbagisix.expense.dto.ExpenseDTO;

public interface ExpenseService {
	ExpenseDTO createExpense(ExpenseDTO expenseDTO);

	ExpenseDTO getExpenseById(Long expenditureId, Long userId);

	List<ExpenseDTO> getExpensesByUserId(Long userId);

	ExpenseDTO updateExpense(Long expenditureId, ExpenseDTO expenseDTO, Long userId);

	void deleteExpense(Long expenditureId, Long userId);

	List<ExpenseVO> getRecentExpenses(Long userId);

	// 시스템 내부 호출용 메서드 (권한 검증 없음)
	ExpenseDTO getExpenseByIdInternal(Long expenditureId);
	ExpenseDTO updateExpenseInternal(Long expenditureId, ExpenseDTO expenseDTO);
}
