package org.bbagisix.analytics.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bbagisix.category.service.CategoryService;
import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
import org.bbagisix.expense.domain.ExpenseVO;
import org.bbagisix.expense.service.ExpenseService;
import org.springframework.beans.factory.annotation.Value;
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
	
	@Value("${LLM_SERVER_URL}")
	private String llmServerUrl;

	@Override
	public List<Long> getTopCategories(Long userId) {

		try {
			List<ExpenseVO> expenses = expenseService.getRecentExpenses(userId); // 최근 2달 소비내역

			// FastAPI 서버가 요구하는 형태로 변환
			List<Map<String, Object>> expsForAnalytics = expenses.stream()
				.map(e -> Map.<String, Object>of(
					"category_id", e.getCategoryId(),
					"amount", e.getAmount(),
					"expenditure_date", e.getExpenditureDate()
				))
				.collect(Collectors.toList());

			Map<String, Object> payload = Map.of("exps", expsForAnalytics);

			String analysisUrl = llmServerUrl + "/analysis";
			Map<String, Object> response = restTemplate.postForObject(analysisUrl, payload, Map.class);
			if (response == null || !response.containsKey("results")) {
				throw new BusinessException(ErrorCode.LLM_ANALYTICS_ERROR);
			}

			List<Number> resultsRaw = (List<Number>)response.get("results");
			List<Long> results = resultsRaw.stream()
				.map(Number::longValue)
				.toList();

			return results;

		} catch (BusinessException e) {
			log.warn("비즈니스 예외 발생: code={}, message={}", e.getCode(), e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("메시지 저장 중 예상하지 못한 오류: ", e);
			throw new BusinessException(ErrorCode.DATA_ACCESS_ERROR, e);
		}

	}
}
