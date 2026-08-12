package com.stockresearch.copilot.parser;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChunkDraft {

	private int chunkIndex;
	private String titlePath;
	private String section;
	private String content;
	private Integer pageNo;
	private int tokenCount;
}
