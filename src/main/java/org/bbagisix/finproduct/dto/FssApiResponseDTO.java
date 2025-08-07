package org.bbagisix.finproduct.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FssApiResponseDTO {

    @JsonProperty("result")
    private ResultDTO result;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultDTO {
        @JsonProperty("prdt_div")
        private String prdtDiv;

        @JsonProperty("total_count")
        private int totalCount;

        @JsonProperty("max_page_no")
        private int maxPageNo;

        @JsonProperty("now_page_no")
        private int nowPageNo;

        @JsonProperty("err_cd")
        private String errCd;

        @JsonProperty("err_msg")
        private String errMsg;

        @JsonProperty("baseList")
        private List<ProductBaseDTO> baseList;

        @JsonProperty("optionList")
        private List<ProductOptionDTO> optionList;
    }
}
