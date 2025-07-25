package org.bbagisix.analytics.service;

import java.util.List;

import org.bbagisix.category.dto.CategoryDTO;

public interface AnalyticsService {
	List<CategoryDTO> getTopCategories(Long userId);
}
