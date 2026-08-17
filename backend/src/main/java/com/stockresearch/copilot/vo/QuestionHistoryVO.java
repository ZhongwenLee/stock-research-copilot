package com.stockresearch.copilot.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QuestionHistoryVO {

	private Long questionId;
	private String question;
	private String answer;
	private String intentType;
	private Long companyId;
	private String companyName;
	private String stockCode;
	private List<String> preferredDocTypes;
	private boolean insufficientEvidence;
	private Long latencyMs;
	private Integer citationCount;
	private LocalDateTime createdAt;
}
