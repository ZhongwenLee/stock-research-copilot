package com.stockresearch.copilot.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CitationVO {

	private Long chunkId;
	private Long documentId;
	private String documentTitle;
	private String docType;
	private String quoteText;
	private Integer rankNo;
	private Double score;
	private String titlePath;
	private Integer pageNo;
	private String section;
}
