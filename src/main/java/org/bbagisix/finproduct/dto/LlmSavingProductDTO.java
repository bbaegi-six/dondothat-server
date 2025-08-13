package org.bbagisix.finproduct.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
// 적금 상품 정보
public class LlmSavingProductDTO {
    private String finPrdtCd;
    private String korCoNm;
    private String finPrdtNm;
    private String spclCnd;
    private String joinMember;
    private double intrRate;
    private double intrRate2;
}