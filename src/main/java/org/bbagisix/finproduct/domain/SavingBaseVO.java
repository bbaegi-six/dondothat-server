package org.bbagisix.finproduct.domain;

import lombok.Getter;
import lombok.ToString;

import java.sql.Timestamp;

@Getter
@ToString
public class SavingBaseVO {
    private Long savingBaseId;
    private String dclsMonth;
    private String finCoNo;
    private String finPrdtCd;
    private String korCoNm;
    private String finPrdtNm;
    private String joinWay;
    private String mtrtInt;
    private String spclCnd;
    private String joinDeny;
    private String joinMember;
    private String etcNote;
    private Integer maxLimit;
    private String dclsStrtDay;
    private String dclsEndDay;
    private String finCoSubmDay;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}