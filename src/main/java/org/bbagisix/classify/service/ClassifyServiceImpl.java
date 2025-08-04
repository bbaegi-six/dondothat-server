package org.bbagisix.classify.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
import org.bbagisix.expense.dto.ExpenseDTO;
import org.bbagisix.expense.service.ExpenseService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class ClassifyServiceImpl implements ClassifyService {

	private final ExpenseService expenseService;
	private final RestTemplate restTemplate = new RestTemplate();
	private static final String SINGLE_URL = "http://llm-server:8000/classify";
	private static final String BATCH_URL = "http://llm-server:8000/classify_batch";

	@Override
	public void classify(ExpenseDTO expense) {

		Map<String, Object> payload = Map.of(
			"expenditure_id", expense.getExpenditureId(),
			"description", expense.getDescription()
		);

		Map<String, Object> response = restTemplate.postForObject(SINGLE_URL, payload, Map.class);

		if (response == null || !response.containsKey("category_id") || !response.containsKey("expenditure_id")) {
			throw new BusinessException(ErrorCode.LLM_CLASSIFY_ERROR);
		}

		Long expenseId = ((Number)response.get("expenditure_id")).longValue();
		Long categoryId = ((Number)response.get("category_id")).longValue();
		expense.setCategoryId(categoryId);
		expenseService.updateExpenseInternal(expenseId, expense);
	}

	@Override
	public void classifyBatch(List<ExpenseDTO> expenses) {

		List<Map<String, Object>> exps = expenses.stream()
			.map(e -> Map.<String, Object>of(
				"expenditure_id", e.getExpenditureId(),
				"description", e.getDescription()
			))
			.collect(Collectors.toList());

		Map<String, Object> payload = Map.<String, Object>of("exps", exps);

		Map<String, Object> response = restTemplate.postForObject(BATCH_URL, payload, Map.class);
		if (response == null || !response.containsKey("results")) {
			throw new BusinessException(ErrorCode.LLM_CLASSIFY_ERROR);
		}

		List<Map<String, Object>> results = (List<Map<String, Object>>)response.get("results");

		// 각 소비내역 업데이트
		for (Map<String, Object> res : results) {
			Long expenseId = ((Number)res.get("expenditure_id")).longValue();
			Long categoryId = ((Number)res.get("category_id")).longValue();

			ExpenseDTO expense = expenseService.getExpenseByIdInternal(expenseId);
			expense.setCategoryId(categoryId);
			expenseService.updateExpenseInternal(expenseId, expense);
		}
	}
}
