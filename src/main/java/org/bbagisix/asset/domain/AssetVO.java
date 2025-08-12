package org.bbagisix.asset.domain;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder(toBuilder = true)
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
	private Date createdAt;
	private String bankAccount;
	private String bankId;
	private String bankPw;
	private String connectedId;
	private String status; // main, sub
}
