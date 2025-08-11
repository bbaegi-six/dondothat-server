package org.bbagisix.tier.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bbagisix.tier.domain.TierVO;

@Mapper
public interface TierMapper {

	// 모든 티어 목록 조회 (tier_order 순서대로)
	List<TierVO> findAllTiers();

	// 티어 ID로 단일 티어 조회
	TierVO findById(@Param("tierId") Long tierId);
}
