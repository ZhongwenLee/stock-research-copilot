package com.stockresearch.copilot.service;

public interface DocumentIngestService {

	void ingestAsync(Long documentId);

	void ingest(Long documentId);
}
