package com.stockresearch.copilot.rag.vector;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class InMemoryVectorStore implements VectorStore {

	private final Map<String, VectorRecord> store = new ConcurrentHashMap<>();

	@Override
	public void upsert(List<VectorRecord> records) {
		if (records == null || records.isEmpty()) {
			return;
		}
		for (VectorRecord record : records) {
			store.put(record.getVectorId(), record);
		}
		log.debug("in-memory vector upsert size={} total={}", records.size(), store.size());
	}

	@Override
	public void deleteByDocumentId(Long documentId) {
		store.entrySet().removeIf(entry -> documentId.equals(entry.getValue().getDocumentId()));
	}

	@Override
	public List<VectorSearchHit> search(float[] queryEmbedding, VectorSearchFilter filter, int topK) {
		if (queryEmbedding == null || queryEmbedding.length == 0 || topK <= 0) {
			return List.of();
		}
		Long companyId = filter == null ? null : filter.getCompanyId();
		Set<Long> documentIds = filter == null ? null : filter.getDocumentIds();

		List<VectorSearchHit> hits = new ArrayList<>();
		for (VectorRecord record : store.values()) {
			if (companyId != null && !companyId.equals(record.getCompanyId())) {
				continue;
			}
			if (documentIds != null && !documentIds.isEmpty() && !documentIds.contains(record.getDocumentId())) {
				continue;
			}
			if (record.getEmbedding() == null || record.getEmbedding().length != queryEmbedding.length) {
				continue;
			}
			double score = cosineSimilarity(queryEmbedding, record.getEmbedding());
			hits.add(VectorSearchHit.builder()
					.vectorId(record.getVectorId())
					.chunkId(record.getChunkId())
					.documentId(record.getDocumentId())
					.companyId(record.getCompanyId())
					.score(score)
					.build());
		}
		hits.sort(Comparator.comparingDouble(VectorSearchHit::getScore).reversed());
		if (hits.size() > topK) {
			return new ArrayList<>(hits.subList(0, topK));
		}
		return hits;
	}

	public int size() {
		return store.size();
	}

	static double cosineSimilarity(float[] a, float[] b) {
		double dot = 0;
		double normA = 0;
		double normB = 0;
		for (int i = 0; i < a.length; i++) {
			dot += a[i] * b[i];
			normA += a[i] * a[i];
			normB += b[i] * b[i];
		}
		if (normA == 0 || normB == 0) {
			return 0;
		}
		return dot / (Math.sqrt(normA) * Math.sqrt(normB));
	}
}
