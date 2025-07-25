package org.bbagisix.classify.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassifyRequest {

	@JsonProperty("expenditure_id")
	private Long expenditureId;

	private String description;
}
