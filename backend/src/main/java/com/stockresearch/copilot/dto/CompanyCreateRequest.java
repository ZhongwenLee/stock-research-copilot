package com.stockresearch.copilot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompanyCreateRequest {

	@NotBlank(message = "stockCode is required")
	private String stockCode;

	@NotBlank(message = "name is required")
	private String name;

	private String exchange;

	private String industry;
}
