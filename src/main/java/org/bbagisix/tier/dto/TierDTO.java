package org.bbagisix.tier.dto;

import org.bbagisix.tier.domain.TierVO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierDTO {
	private Long tierId;
	private String name;
	private Boolean isCurrentTier; // 사용자의 현재 티어인지 여부

	// TierVO를 TierDTO로 변환하는 정적 메서드
	public static TierDTO from(TierVO tierVO) {
		return TierDTO.builder()
			.tierId(tierVO.getTierId())
			.name(tierVO.getName())
			.build();
	}

	// 사용자 정보를 포함한 TierDTO 생성
	public static TierDTO fromWithUserInfo(TierVO tierVO, Long userCurrentTierId) {
		return TierDTO.builder()
			.tierId(tierVO.getTierId())
			.name(tierVO.getName())
			.isCurrentTier(tierVO.getTierId().equals(userCurrentTierId))
			.build();
	}
}
