package com.stockresearch.copilot.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SummaryHistoryVO {

	private Long summaryId;
	private Long companyId;
	private String companyName;
	private String stockCode;
	private String mode;
	private String title;
	private String overview;
	private List<String> docTypes;
	private LocalDate startDate;
	private LocalDate endDate;
	private Long latencyMs;
	private boolean insufficientEvidence;
	private Integer citationCount;
	private LocalDateTime createdAt;
}
