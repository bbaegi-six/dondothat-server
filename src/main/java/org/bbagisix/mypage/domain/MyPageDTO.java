package org.bbagisix.mypage.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MyPageDTO {
	private AccountInfo mainAccount;
	private AccountInfo subAccount;
	private TierInfo tierInfo;

	@Getter
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor
	public static class AccountInfo {
		private Long assetId;
		private String assetName;
		private String bankName;
		private Long balance;
		private String status;    // main, sub
	}

	@Getter
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor
	public static class TierInfo {
		private Long tierId;
		private String tierName;
		private Integer completedChallenges;
	}
}
