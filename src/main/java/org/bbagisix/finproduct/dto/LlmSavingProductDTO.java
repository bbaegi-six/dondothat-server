package org.bbagisix.finproduct.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// LLM 서버와 통신용 적금 상품 정보 (요청/응답 공용)
public class LlmSavingProductDTO {
    private String finPrdtCd;
    private String korCoNm;
    private String finPrdtNm;
    private String spclCnd;
    private String joinMember;
    private double intrRate;
    private double intrRate2;
}