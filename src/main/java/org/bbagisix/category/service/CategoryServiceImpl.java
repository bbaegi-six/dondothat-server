package org.bbagisix.category.service;

import java.util.List;
import java.util.stream.Collectors;

import org.bbagisix.category.domain.CategoryVO;
import org.bbagisix.category.dto.CategoryDTO;
import org.bbagisix.category.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

	private final CategoryMapper categoryMapper;

	@Override
	public List<CategoryDTO> getAllCategories() {
		return categoryMapper.findAll()
			.stream()
			.map(vo -> new CategoryDTO(vo.getCategoryId(), vo.getName(), vo.getIcon()))
			.collect(Collectors.toList());
	}

	@Override
	public CategoryDTO getCategoryById(Long id) {

		CategoryVO vo = categoryMapper.findById(id);
		return new CategoryDTO(vo.getCategoryId(), vo.getName(), vo.getIcon());
	}
}
