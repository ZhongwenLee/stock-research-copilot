package com.stockresearch.copilot.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentChunkVO {

	private Long id;
	private Long documentId;
	private Long companyId;
	private Integer chunkIndex;
	private String titlePath;
	private String content;
	private Integer pageNo;
	private String section;
	private Integer tokenCount;
	private String vectorId;
	private LocalDateTime createdAt;
}
