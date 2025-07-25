package org.bbagisix.codef.domain;

import java.sql.Timestamp;

import lombok.Getter;
import lombok.Setter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CodefAccessTokenVO {
	// access_token db
	private Long tokenId;
	private String accessToken;
	private Timestamp expiresAt;
	private Timestamp updatedAt;
	private Timestamp createdAt;
}