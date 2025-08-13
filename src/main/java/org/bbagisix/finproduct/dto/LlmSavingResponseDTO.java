package org.bbagisix.finproduct.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
// LLM 서버 응답 데이터
public class LlmSavingResponseDTO {
    private List<RecommendedSavingDTO> recommendations;
}