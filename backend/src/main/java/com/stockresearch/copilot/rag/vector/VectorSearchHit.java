package com.stockresearch.copilot.rag.vector;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VectorSearchHit {

	private String vectorId;
	private Long chunkId;
	private Long documentId;
	private Long companyId;
	/** Cosine similarity in [0, 1] preferred; higher is better. */
	private double score;
}
