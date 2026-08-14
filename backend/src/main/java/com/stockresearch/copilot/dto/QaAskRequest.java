package com.stockresearch.copilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class QaAskRequest {

	@NotBlank
	@Size(max = 2000)
	private String question;

	/** Preferred company id; optional if stockCode or name can be resolved from question. */
	private Long companyId;

	/** Optional stock code override, e.g. 600519. */
	private String stockCode;

	/** Optional document type filter: FINANCIAL_REPORT / ANNOUNCEMENT / RESEARCH_REPORT. */
	private List<String> docTypes;

	/** Max chunks after rerank; default from config. */
	private Integer topK;

	/** Reserved for multi-turn; ignored in Step 3. */
	private String conversationId;
}
