package org.bbagisix.classify.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
import org.bbagisix.expense.domain.ExpenseVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class ClassifyServiceImpl implements ClassifyService {

	private final RestTemplate restTemplate = new RestTemplate();
	
	@Value("${LLM_SERVER_URL}")
	private String llmServerUrl;

	@Override
	public List<ExpenseVO> classify(List<ExpenseVO> expenses) {
		AtomicLong counter = new AtomicLong(1);

		// categoryId == 14인 것만 분류 대상
		List<ExpenseVO> toClassify = expenses.stream()
			.filter(e -> e.getCategoryId() != null && e.getCategoryId() == 14)
			.collect(Collectors.toList());

		// 분류 요청에 쓸 payload 구성
		List<Map<String, Object>> exps = toClassify.stream()
			.map(e -> {
				long id = counter.getAndIncrement();
				e.setExpenditureId(id);
				return Map.<String, Object>of(
					"expenditure_id", id,
					"description", e.getDescription()
				);
			})
			.collect(Collectors.toList());

		// 아무것도 보낼 게 없다면 바로 리턴
		if (exps.isEmpty()) {
			return expenses;
		}

		Map<String, Object> payload = Map.of("exps", exps);

		String classifyUrl = llmServerUrl + "/classify";
		Map<String, Object> response = restTemplate.postForObject(classifyUrl, payload, Map.class);
		if (response == null || !response.containsKey("results")) {
			throw new BusinessException(ErrorCode.LLM_CLASSIFY_ERROR);
		}

		List<Map<String, Object>> results = (List<Map<String, Object>>)response.get("results");

		// ID 매칭용 map
		Map<Long, Long> idToCategory = results.stream()
			.collect(Collectors.toMap(
				r -> ((Number)r.get("expenditure_id")).longValue(),
				r -> ((Number)r.get("category_id")).longValue()
			));

		// 다시 카테고리 업데이트
		for (ExpenseVO expense : toClassify) {
			Long id = expense.getExpenditureId();
			Long categoryId = idToCategory.get(id);
			if (categoryId != null) {
				expense.setCategoryId(categoryId);
				expense.setExpenditureId(null); // 임시 ID 초기화
			}
		}

		return expenses;
	}
}
