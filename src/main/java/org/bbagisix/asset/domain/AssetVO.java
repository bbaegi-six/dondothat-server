package org.bbagisix.asset.domain;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AssetVO {
	// asset db
	private Long assetId;
	private Long userId;
	private String assetName;
	private Long balance;
	private String bankName;
	private String bankAccount;
	private String bankId;
	private String bankPw;
	private String connectedId;
}
