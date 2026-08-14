package com.stockresearch.copilot.rag.retrieve;

import com.stockresearch.copilot.entity.DocumentChunk;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RetrievedChunk {

	private Long chunkId;
	private Long documentId;
	private Long companyId;
	private DocumentChunk chunk;
	private double vectorScore;
	private double keywordScore;
	private double fusedScore;
	private String source; // vector | keyword | hybrid
}
