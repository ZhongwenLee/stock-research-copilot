package com.stockresearch.copilot.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SummarySectionVO {

	private String title;
	private String content;
	private Integer charCount;
	private Integer citationCount;
}
