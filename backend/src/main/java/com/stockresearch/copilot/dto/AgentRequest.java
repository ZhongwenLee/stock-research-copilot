package com.stockresearch.copilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AgentRequest {

	@NotBlank
	@Size(max = 2000)
	private String question;

	private Long companyId;

	private String stockCode;

	@Size(max = 16)
	private List<String> docTypes;

	private Integer topK;

	private String summaryMode;

	private LocalDate startDate;

	private LocalDate endDate;
}
