package com.stockresearch.copilot.rag.vector;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
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

	public int size() {
		return store.size();
	}
}
