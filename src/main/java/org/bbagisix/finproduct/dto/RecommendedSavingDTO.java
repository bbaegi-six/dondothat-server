package org.bbagisix.finproduct.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedSavingDTO {
    private String finPrdtCd;      // 금융상품 코드
    private String korCoNm;        // 금융회사 명
    private String finPrdtNm;      // 금융상품 명
    private String spclCnd;        // 우대 조건
    private String joinMember;     // 가입 대상
    private BigDecimal intrRate;       // 저축 금리
    private BigDecimal intrRate2;      // 최고 우대금리
    
    // 사용자 정보 (LLM 추천용)
    private Integer userAge;       // 사용자 나이
    private String userJob;        // 사용자 직업
    private String mainBankName;   // 주거래은행
}
