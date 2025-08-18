package org.bbagisix.common.codef.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CodefTransactionReqDTO {
	private String bankCode; // organization
	private String bankId;
	private String bankEncryptPw;
	private String bankAccount;

	private String connectedId;
	private String startDate;
	private String endDate;
	private String orderBy;
}
