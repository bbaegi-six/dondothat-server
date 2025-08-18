package org.bbagisix.common.codef.domain;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CodefAccessTokenVO {
	// access_token db
	private Long tokenId;
	private String accessToken;
	private Date expiresAt;
	private Date updatedAt;
	private Date createdAt;
}