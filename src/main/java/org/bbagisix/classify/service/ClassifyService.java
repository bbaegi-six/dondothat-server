package org.bbagisix.classify.service;

import java.util.List;

import org.bbagisix.expense.dto.ExpenseDTO;

public interface ClassifyService {
	void classify(ExpenseDTO expense);

	void classifyBatch(List<ExpenseDTO> expense);
}
