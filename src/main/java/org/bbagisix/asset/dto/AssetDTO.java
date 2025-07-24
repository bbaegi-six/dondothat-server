package org.bbagisix.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AssetDTO {
	private String bankName;
	private String bankId;
	private String bankpw;
	private String bankAccount;
}
