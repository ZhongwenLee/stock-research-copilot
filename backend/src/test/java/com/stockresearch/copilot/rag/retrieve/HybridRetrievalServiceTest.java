package com.stockresearch.copilot.rag.retrieve;

import com.stockresearch.copilot.common.enums.IntentType;
import com.stockresearch.copilot.common.metrics.RagMetrics;
import com.stockresearch.copilot.entity.DocumentChunk;
import com.stockresearch.copilot.mapper.DocumentChunkMapper;
import com.stockresearch.copilot.rag.embedding.EmbeddingClient;
import com.stockresearch.copilot.rag.intent.QuestionIntent;
import com.stockresearch.copilot.rag.vector.VectorSearchHit;
import com.stockresearch.copilot.rag.vector.VectorStore;
import com.stockresearch.copilot.service.ReadyDocumentLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HybridRetrievalServiceTest {

	@Mock
	private EmbeddingClient embeddingClient;
	@Mock
	private VectorStore vectorStore;
	@Mock
	private DocumentChunkMapper documentChunkMapper;
	@Mock
	private ReadyDocumentLookup readyDocumentLookup;
	@Mock
	private RagMetrics ragMetrics;

	private HybridRetrievalService service;

	@BeforeEach
	void setUp() {
		service = new HybridRetrievalService(
				embeddingClient, vectorStore, documentChunkMapper, readyDocumentLookup, ragMetrics);
	}

	@Test
	void returnsEmptyWhenCompanyHasNoReadyDocs() {
		QuestionIntent intent = QuestionIntent.builder()
				.intentType(IntentType.QA)
				.companyId(1L)
				.rawQuestion("营收多少")
				.build();
		when(readyDocumentLookup.findReadyDocumentIds(eq(1L), any())).thenReturn(Set.of());

		assertTrue(service.retrieve(intent, 10).isEmpty());
		verify(ragMetrics).recordRetrieve(anyLong());
	}

	@Test
	void fusesVectorAndKeywordHits() {
		QuestionIntent intent = QuestionIntent.builder()
				.intentType(IntentType.QA)
				.companyId(1L)
				.rawQuestion("茅台 营收 增长")
				.build();
		when(readyDocumentLookup.findReadyDocumentIds(eq(1L), any())).thenReturn(Set.of(10L));
		when(embeddingClient.embed(any())).thenReturn(List.of(new float[] {0.1f, 0.2f}));
		when(vectorStore.search(any(), any(), anyInt())).thenReturn(List.of(
				VectorSearchHit.builder().chunkId(1L).documentId(10L).companyId(1L).score(0.9).build()
		));

		DocumentChunk keywordChunk = new DocumentChunk();
		keywordChunk.setId(2L);
		keywordChunk.setDocumentId(10L);
		keywordChunk.setCompanyId(1L);
		keywordChunk.setContent("茅台营收持续增长");
		keywordChunk.setTitlePath("财务");
		when(documentChunkMapper.selectList(any())).thenReturn(List.of(keywordChunk));

		List<RetrievedChunk> result = service.retrieve(intent, 5);

		assertFalse(result.isEmpty());
		assertTrue(result.stream().anyMatch(item -> item.getChunkId().equals(1L) || item.getChunkId().equals(2L)));
		verify(ragMetrics).recordRetrieve(anyLong());
	}

	@Test
	void extractTermsSkipsStopWords() {
		List<String> terms = service.extractTerms("请 帮我 看一下 营收 情况");
		assertTrue(terms.contains("营收"));
		assertFalse(terms.contains("请"));
		assertFalse(terms.contains("帮我"));
	}
}
