package org.bbagisix.finproduct.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ProductBaseDTO {

    @JsonProperty("dcls_month")
    private String dclsMonth;

    @JsonProperty("fin_co_no")
    private String finCoNo;

    @JsonProperty("fin_prdt_cd")
    private String finPrdtCd;

    @JsonProperty("kor_co_nm")
    private String korCoNm;

    @JsonProperty("fin_prdt_nm")
    private String finPrdtNm;

    @JsonProperty("join_way")
    private String joinWay;

    @JsonProperty("mtrt_int")
    private String mtrtInt;

    @JsonProperty("spcl_cnd")
    private String spclCnd;

    @JsonProperty("join_deny")
    private String joinDeny;

    @JsonProperty("join_member")
    private String joinMember;

    @JsonProperty("etc_note")
    private String etcNote;

    @JsonProperty("max_limit")
    private Integer maxLimit;

    @JsonProperty("dcls_strt_day")
    private String dclsStrtDay;

    @JsonProperty("dcls_end_day")
    private String dclsEndDay;

    @JsonProperty("fin_co_subm_day")
    private String finCoSubmDay;
}