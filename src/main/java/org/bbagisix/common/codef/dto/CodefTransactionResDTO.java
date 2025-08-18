package org.bbagisix.common.codef.dto;

import java.util.List;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CodefTransactionResDTO {
	private String resAccountBalance; // 계좌 잔액
	private String resAccountName;  // 계좌명
	private List<HistoryItem> resTrHistoryList;  // 거래내역 리스트

	@Getter
	@Setter
	@AllArgsConstructor
	@NoArgsConstructor
	@ToString
	public static class HistoryItem {
		private String resAccountTrDate; // 날짜
		private String resAccountTrTime; // 시간
		private String resAccountOut; // 출금액
		private String resAccountIn; // 입금액
		private String resAccountDesc1; // 거래 설명
		private String resAccountDesc2; // 거래 설명 (거래 수단)
		private String resAccountDesc3; // 거래 설명 (사업자명)
		private String resAccountDesc4; // 거래 설명 (위치)
		private String resAfterTranBalance; // 거래 후 잔액
		private String tranDesc; // 거래 설명
	}
}
