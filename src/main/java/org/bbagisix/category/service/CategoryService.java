package org.bbagisix.category.service;

import java.util.List;

import org.bbagisix.category.dto.CategoryDTO;

public interface CategoryService {
	List<CategoryDTO> getAllCategories();
	CategoryDTO getCategoryById(Long id);
}
