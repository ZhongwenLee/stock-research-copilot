package com.stockresearch.copilot.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Micrometer timers/counters for RAG critical path stages.
 */
@Slf4j
@Component
public class RagMetrics {

	private final Timer retrieveTimer;
	private final Timer rerankTimer;
	private final Timer generateTimer;
	private final Timer embedTimer;
	private final Timer ingestTimer;
	private final Counter qaSuccessCounter;
	private final Counter qaFailureCounter;
	private final Counter ingestFailureCounter;
	private final Counter insufficientEvidenceCounter;

	public RagMetrics(MeterRegistry registry) {
		this.retrieveTimer = Timer.builder("rag.retrieve")
				.description("Hybrid retrieval latency")
				.register(registry);
		this.rerankTimer = Timer.builder("rag.rerank")
				.description("Rerank latency")
				.register(registry);
		this.generateTimer = Timer.builder("rag.generate")
				.description("LLM generation latency")
				.register(registry);
		this.embedTimer = Timer.builder("rag.embed")
				.description("Embedding batch latency")
				.register(registry);
		this.ingestTimer = Timer.builder("rag.ingest")
				.description("Document ingest latency")
				.register(registry);
		this.qaSuccessCounter = Counter.builder("rag.qa.success")
				.description("Successful QA requests")
				.register(registry);
		this.qaFailureCounter = Counter.builder("rag.qa.failure")
				.description("Failed QA requests")
				.register(registry);
		this.ingestFailureCounter = Counter.builder("rag.ingest.failure")
				.description("Failed document ingest jobs")
				.register(registry);
		this.insufficientEvidenceCounter = Counter.builder("rag.insufficient_evidence")
				.description("Answers with insufficient evidence")
				.register(registry);
	}

	public void recordRetrieve(long latencyMs) {
		retrieveTimer.record(latencyMs, TimeUnit.MILLISECONDS);
	}

	public void recordRerank(long latencyMs) {
		rerankTimer.record(latencyMs, TimeUnit.MILLISECONDS);
	}

	public void recordGenerate(long latencyMs) {
		generateTimer.record(latencyMs, TimeUnit.MILLISECONDS);
	}

	public void recordEmbed(long latencyMs) {
		embedTimer.record(latencyMs, TimeUnit.MILLISECONDS);
	}

	public void recordIngest(long latencyMs) {
		ingestTimer.record(latencyMs, TimeUnit.MILLISECONDS);
	}

	public void markQaSuccess(boolean insufficientEvidence) {
		qaSuccessCounter.increment();
		if (insufficientEvidence) {
			insufficientEvidenceCounter.increment();
		}
	}

	public void markQaFailure() {
		qaFailureCounter.increment();
	}

	public void markIngestFailure(Long documentId, String reason) {
		ingestFailureCounter.increment();
		log.error("ALERT ingest_failed documentId={} reason={}", documentId, reason);
	}

	public void markAiUnavailable(String operation, String reason) {
		log.error("ALERT ai_unavailable operation={} reason={}", operation, reason);
	}
}
