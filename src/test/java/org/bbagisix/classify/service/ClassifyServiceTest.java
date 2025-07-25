package org.bbagisix.classify.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.bbagisix.config.TestRootConfig;
import org.bbagisix.expense.dto.ExpenseDTO;
import org.bbagisix.expense.service.ExpenseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.log4j.Log4j2;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestRootConfig.class})
@Transactional
@Log4j2
class ClassifyServiceTest {

	@Autowired
	private ClassifyService classifyService;

	@Autowired
	private ExpenseService expenseService;

	private ExpenseDTO createTestDTO(String desc) {
		return ExpenseDTO.builder()
			.userId(1L)
			.categoryId(14L) // 카테고리 미분류
			.assetId(1L)
			.amount(15000L)
			.description(desc)
			.expenditureDate(new java.util.Date())
			.build();
	}

	@Test
	void classify() {
		//given
		ExpenseDTO expense = createTestDTO("인터파크티켓");
		Long expenseId = expenseService.createExpense(expense).getExpenditureId();
		expense.setExpenditureId(expenseId);

		//when
		classifyService.classify(expense);

		//then
		Long categoryId = expenseService.getExpenseById(expenseId).getCategoryId();
		assertEquals(8, categoryId); // 카테고리(8)로 잘 분류됐는지 확인
	}

	@Test
	void classifyBatch() {

		//given
		String[] desc = {"맥도날드", "인터파크티켓", "역전할머니맥주", "우아한형제들", "신세계백화점"};
		List<ExpenseDTO> exps = new ArrayList<>();

		for (int i = 0; i < 5; i++) {
			ExpenseDTO expense = createTestDTO(desc[i]);
			Long expenseId = expenseService.createExpense(expense).getExpenditureId();
			expense.setExpenditureId(expenseId);
			exps.add(expense);
		}

		//when
		classifyService.classifyBatch(exps);

		//then
		for (ExpenseDTO expense : exps) {
			ExpenseDTO saved = expenseService.getExpenseById(expense.getExpenditureId());
			assertNotEquals(14L, saved.getCategoryId(), "여전히 미분류 상태임");
			log.info(saved.getDescription() + " 분류된 카테고리: " + saved.getCategoryId());
		}

	}
}