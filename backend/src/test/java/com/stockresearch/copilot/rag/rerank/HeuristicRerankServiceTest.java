package com.stockresearch.copilot.rag.rerank;

import com.stockresearch.copilot.common.metrics.RagMetrics;
import com.stockresearch.copilot.entity.DocumentChunk;
import com.stockresearch.copilot.mapper.DocumentChunkMapper;
import com.stockresearch.copilot.rag.retrieve.RetrievedChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeuristicRerankServiceTest {

	@Mock
	private DocumentChunkMapper documentChunkMapper;
	@Mock
	private RagMetrics ragMetrics;

	private HeuristicRerankService service;

	@BeforeEach
	void setUp() {
		service = new HeuristicRerankService(documentChunkMapper, ragMetrics);
	}

	@Test
	void returnsEmptyWhenNoCandidates() {
		assertTrue(service.rerank("业绩", List.of(), 5).isEmpty());
		verify(ragMetrics).recordRerank(anyLong());
	}

	@Test
	void ranksHigherOverlapFirstAndRespectsTopK() {
		DocumentChunk weak = chunk(1L, "无关内容", "附录");
		DocumentChunk strong = chunk(2L, "公司业绩大幅增长", "经营分析");
		List<RetrievedChunk> candidates = List.of(
				RetrievedChunk.builder().chunkId(1L).chunk(weak).fusedScore(0.2).vectorScore(0.2).keywordScore(0.1).build(),
				RetrievedChunk.builder().chunkId(2L).chunk(strong).fusedScore(0.3).vectorScore(0.3).keywordScore(0.2).build()
		);

		List<RetrievedChunk> ranked = service.rerank("业绩增长", candidates, 1);

		assertEquals(1, ranked.size());
		assertEquals(2L, ranked.get(0).getChunkId());
		verify(documentChunkMapper, never()).selectList(any());
		verify(ragMetrics).recordRerank(anyLong());
	}

	@Test
	void enrichesMissingChunksFromMapper() {
		DocumentChunk loaded = chunk(9L, "营收同比增长", "财务");
		when(documentChunkMapper.selectList(any())).thenReturn(List.of(loaded));

		List<RetrievedChunk> ranked = service.rerank("营收", List.of(
				RetrievedChunk.builder().chunkId(9L).fusedScore(0.5).vectorScore(0.4).keywordScore(0.3).build()
		), 3);

		assertEquals(1, ranked.size());
		assertEquals("营收同比增长", ranked.get(0).getChunk().getContent());
	}

	private DocumentChunk chunk(Long id, String content, String titlePath) {
		DocumentChunk chunk = new DocumentChunk();
		chunk.setId(id);
		chunk.setDocumentId(100L);
		chunk.setContent(content);
		chunk.setTitlePath(titlePath);
		return chunk;
	}
}
