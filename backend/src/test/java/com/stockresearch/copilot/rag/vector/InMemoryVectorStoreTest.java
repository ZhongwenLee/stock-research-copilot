package com.stockresearch.copilot.rag.vector;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryVectorStoreTest {

	@Test
	void searchReturnsClosestByCosine() {
		InMemoryVectorStore store = new InMemoryVectorStore();
		store.upsert(List.of(
				VectorRecord.builder()
						.vectorId("v1")
						.chunkId(1L)
						.documentId(10L)
						.companyId(100L)
						.embedding(new float[] {1f, 0f, 0f})
						.build(),
				VectorRecord.builder()
						.vectorId("v2")
						.chunkId(2L)
						.documentId(10L)
						.companyId(100L)
						.embedding(new float[] {0.9f, 0.1f, 0f})
						.build(),
				VectorRecord.builder()
						.vectorId("v3")
						.chunkId(3L)
						.documentId(11L)
						.companyId(200L)
						.embedding(new float[] {1f, 0f, 0f})
						.build()));

		List<VectorSearchHit> hits = store.search(
				new float[] {1f, 0f, 0f},
				VectorSearchFilter.builder().companyId(100L).documentIds(Set.of(10L)).build(),
				5);

		assertEquals(2, hits.size());
		assertEquals(1L, hits.get(0).getChunkId());
		assertTrue(hits.get(0).getScore() >= hits.get(1).getScore());
	}
}
