package org.bbagisix.asset.domain;

import java.sql.Timestamp;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AssetVO {
	// asset db
	private Long assetId;
	private Long userId;
	private String assetName;
	private Long balance;
	private String bankName;
	private Timestamp createdAt;
	private String bankAccount;
	private String bankId;
	private String bankPw;
	private String connectedId;
}
