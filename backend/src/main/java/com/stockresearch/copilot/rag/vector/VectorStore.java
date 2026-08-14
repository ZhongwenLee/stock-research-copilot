package com.stockresearch.copilot.rag.vector;

import java.util.List;

public interface VectorStore {

	void upsert(List<VectorRecord> records);

	void deleteByDocumentId(Long documentId);

	/**
	 * Similarity search. Empty filter means no restriction.
	 */
	List<VectorSearchHit> search(float[] queryEmbedding, VectorSearchFilter filter, int topK);
}
