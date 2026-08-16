package com.stockresearch.copilot.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AgentAnswerVO {

	private String intentType;
	private String question;
	private String answer;
	private Long companyId;
	private String companyName;
	private String stockCode;
	private List<String> preferredDocTypes;
	private List<AgentStepVO> steps;
	private List<CitationVO> citations;
	private List<DocumentChunkVO> chunks;
	private Long latencyMs;
	private boolean insufficientEvidence;
}
