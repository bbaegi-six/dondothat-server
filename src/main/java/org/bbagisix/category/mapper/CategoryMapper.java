package org.bbagisix.category.mapper;

import java.util.List;

import org.bbagisix.category.domain.CategoryVO;

public interface CategoryMapper {
	// 모든 카테고리 목록 조회
	List<CategoryVO> findAll();

	// id를 기준으로 카테고리 정보 조회
	CategoryVO findById(Long categoryId);
}
