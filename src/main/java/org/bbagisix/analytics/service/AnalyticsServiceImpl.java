package org.bbagisix.analytics.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bbagisix.category.dto.CategoryDTO;
import org.bbagisix.category.service.CategoryService;
import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
import org.bbagisix.expense.domain.ExpenseVO;
import org.bbagisix.expense.service.ExpenseService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

	private final ExpenseService expenseService;
	private final CategoryService categoryService;
	private final RestTemplate restTemplate = new RestTemplate();
	private static final String URL = "http://llm-server:8000/analysis";
	private static final String LOCAL_URL = "http://localhost:8000/analysis"; // 로컬 테스트용

	@Override
	public List<CategoryDTO> getTopCategories(Long userId) {

		try {
			List<ExpenseVO> expenses = expenseService.getRecentExpenses(userId);

			Map<String, Object> payload = Map.of("exps", expenses);

			Map<String, Object> response = restTemplate.postForObject(URL, payload, Map.class);
			if (response == null || !response.containsKey("results")) {
				throw new BusinessException(ErrorCode.LLM_ANALYTICS_ERROR);
			}

			List<Number> resultsRaw = (List<Number>)response.get("results");
			List<Long> results = resultsRaw.stream()
				.map(Number::longValue)
				.toList();

			List<CategoryDTO> categories = new ArrayList<>();
			for (Long id : results) {
				CategoryDTO dto = categoryService.getCategoryById(id);
				if (dto != null) {
					categories.add(dto);
				}
			}
			return categories;

		} catch (BusinessException e) {
			log.warn("비즈니스 예외 발생: code={}, message={}", e.getCode(), e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("메시지 저장 중 예상하지 못한 오류: ", e);
			throw new BusinessException(ErrorCode.DATA_ACCESS_ERROR, e);
		}

	}
}
