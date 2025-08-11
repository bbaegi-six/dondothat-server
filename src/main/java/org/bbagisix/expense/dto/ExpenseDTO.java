package org.bbagisix.expense.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDTO {
	// 응답 필드 (서버가 클라이언트에게 응답을 보낼떄만 사용)
	private Long expenditureId;
	private String categoryName;
	private String categoryIcon;
	private String assetName;
	private String bankName;
	private Date createdAt;
	private Date updatedAt;

	// 요청/응답 공통 필드
	private Long userId;
	private Long categoryId;
	private Long assetId;
	private Long amount;
	private String description;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	private Date expenditureDate;
}
