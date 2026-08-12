package com.stockresearch.copilot.rag.vector;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class VectorRecord {

	private String vectorId;
	private Long chunkId;
	private Long documentId;
	private Long companyId;
	private float[] embedding;
	private Map<String, Object> metadata;
}
