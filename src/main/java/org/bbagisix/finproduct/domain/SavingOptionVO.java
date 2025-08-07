package org.bbagisix.finproduct.domain;

import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@ToString
public class SavingOptionVO {
    private Long savingOptionId;
    private Long savingBaseId; // Foreign Key
    private String dclsMonth;
    private String finCoNo;
    private String finPrdtCd;
    private String intrRateType;
    private String intrRateTypeNm;
    private String rsrvType;
    private String rsrvTypeNm;
    private String saveTrm;
    private BigDecimal intrRate;
    private BigDecimal intrRate2;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}