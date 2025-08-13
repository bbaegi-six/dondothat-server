package org.bbagisix.saving.dto;

import java.util.Date;

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
@AllArgsConstructor
@NoArgsConstructor
public class SavingDTO {
	private Long categoryId; //
	private String title;
	private Long period;
	private Date startDate;
	private Date endDate;
	private Long saving;
}
