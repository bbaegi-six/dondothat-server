package org.bbagisix.finproduct.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
// LLM 서버로 보내는 추천 요청 데이터 (상품리스트 + 사용자정보)
public class LlmSavingRequestDTO {
    private List<LlmSavingProductDTO> savings;
    private int userAge;
    private String userRole;
    private String mainBankName;
}