package org.bbagisix.analytics.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.bbagisix.category.dto.CategoryDTO;
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
class AnalyticsServiceTest {

	@Autowired
	private AnalyticsService analyticsService;

	@Autowired
	private ExpenseService expenseService;

	private ExpenseDTO createTestDTO(Long categoryId, Long amount) {
		return ExpenseDTO.builder()
			.userId(1L)
			.categoryId(categoryId)
			.assetId(1L)
			.amount(amount)
			.description("test description")
			.expenditureDate(new java.util.Date())
			.build();
	}

	@Test
	void getTopCategories() {

		// given
		for (int i = 1; i < 9; i++) {
			Long amount = 10000L * i;
			expenseService.createExpense(createTestDTO((long)i, amount));
			// 카테고리 1=10000원, 2=20000원, 3=30000원, ... 식의 테스트 데이터 생성
		}

		// when
		List<CategoryDTO> topCategories = analyticsService.getTopCategories(1L);

		// then
		assertEquals(3, topCategories.size()); // 전체 결과 개수가 3개인지 확인
		log.info(topCategories); // 결과 출력
	}
}