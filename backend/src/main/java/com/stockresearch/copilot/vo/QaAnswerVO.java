package com.stockresearch.copilot.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QaAnswerVO {

	private Long questionId;
	private String question;
	private String answer;
	private String intentType;
	private Long companyId;
	private String companyName;
	private String stockCode;
	private List<String> preferredDocTypes;
	private boolean insufficientEvidence;
	private List<CitationVO> citations;
	private List<DocumentChunkVO> chunks;
	private Long latencyMs;
}
