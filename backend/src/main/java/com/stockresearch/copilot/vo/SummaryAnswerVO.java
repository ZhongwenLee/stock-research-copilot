package com.stockresearch.copilot.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class SummaryAnswerVO {

	private Long summaryId;
	private Long companyId;
	private String companyName;
	private String stockCode;
	private String mode;
	private String title;
	private String overview;
	private List<SummarySectionVO> sections;
	private List<CitationVO> citations;
	private List<DocumentChunkVO> chunks;
	private List<String> docTypes;
	private LocalDate startDate;
	private LocalDate endDate;
	private Long latencyMs;
	private boolean insufficientEvidence;
}
