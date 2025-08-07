package org.bbagisix.finproduct.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductOptionDto {

    @JsonProperty("dcls_month")
    private String dclsMonth;

    @JsonProperty("fin_co_no")
    private String finCoNo;

    @JsonProperty("fin_prdt_cd")
    private String finPrdtCd;

    @JsonProperty("intr_rate_type")
    private String intrRateType;

    @JsonProperty("intr_rate_type_nm")
    private String intrRateTypeNm;

    @JsonProperty("rsrv_type")
    private String rsrvType;

    @JsonProperty("rsrv_type_nm")
    private String rsrvTypeNm;

    @JsonProperty("save_trm")
    private String saveTrm;

    @JsonProperty("intr_rate")
    private BigDecimal intrRate;

    @JsonProperty("intr_rate2")
    private BigDecimal intrRate2;
}