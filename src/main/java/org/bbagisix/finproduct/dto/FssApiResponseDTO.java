package org.bbagisix.finproduct.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FssApiResponseDTO {

    @JsonProperty("result")
    private ResultDto result;

    @Getter
    @Setter
    public static class ResultDto {
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