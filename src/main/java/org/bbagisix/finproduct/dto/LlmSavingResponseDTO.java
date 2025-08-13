package org.bbagisix.finproduct.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
// LLM 서버에서 받는 추천 응답 데이터 (추천된 상품리스트)
public class LlmSavingResponseDTO {
    private List<LlmSavingProductDTO> recommendations;
}