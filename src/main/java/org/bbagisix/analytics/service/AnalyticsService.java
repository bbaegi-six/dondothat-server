package org.bbagisix.analytics.service;

import java.util.List;

public interface AnalyticsService {
	List<Long> getTopCategories(Long userId);
}
