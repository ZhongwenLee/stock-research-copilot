package com.stockresearch.copilot.dto;

import com.stockresearch.copilot.common.enums.SummaryMode;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SummaryGenerateRequest {

	@NotNull
	private Long companyId;

	private String stockCode;

	@Size(max = 16)
	private List<String> docTypes;

	private SummaryMode mode = SummaryMode.FAST;

	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate startDate;

	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate endDate;

	/** Final chunk count after rerank/selection. */
	private Integer topK;
}
