package com.stockresearch.copilot.service.impl;

import com.stockresearch.copilot.common.enums.SummaryMode;
import com.stockresearch.copilot.common.metrics.RagMetrics;
import com.stockresearch.copilot.config.AppProperties;
import com.stockresearch.copilot.dto.SummaryGenerateRequest;
import com.stockresearch.copilot.entity.Company;
import com.stockresearch.copilot.entity.DocumentChunk;
import com.stockresearch.copilot.entity.Summary;
import com.stockresearch.copilot.mapper.CitationMapper;
import com.stockresearch.copilot.mapper.CompanyMapper;
import com.stockresearch.copilot.mapper.DocumentMapper;
import com.stockresearch.copilot.mapper.SummaryMapper;
import com.stockresearch.copilot.rag.context.SummaryContext;
import com.stockresearch.copilot.rag.context.SummaryContextBuilder;
import com.stockresearch.copilot.rag.context.SummaryTemplate;
import com.stockresearch.copilot.rag.llm.ChatClient;
import com.stockresearch.copilot.rag.rerank.HeuristicRerankService;
import com.stockresearch.copilot.rag.retrieve.RetrievedChunk;
import com.stockresearch.copilot.rag.retrieve.SummaryAggregationService;
import com.stockresearch.copilot.vo.SummaryAnswerVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryServiceImplTest {

	@Mock private CompanyMapper companyMapper;
	@Mock private SummaryAggregationService summaryAggregationService;
	@Mock private HeuristicRerankService heuristicRerankService;
	@Mock private SummaryContextBuilder summaryContextBuilder;
	@Mock private ChatClient chatClient;
	@Mock private SummaryMapper summaryMapper;
	@Mock private CitationMapper citationMapper;
	@Mock private DocumentMapper documentMapper;
	@Mock private RagMetrics ragMetrics;

	private SummaryServiceImpl service;

	@BeforeEach
	void setUp() {
		AppProperties props = new AppProperties();
		props.getQa().setQuoteMaxChars(100);
		service = new SummaryServiceImpl(
				companyMapper, summaryAggregationService, heuristicRerankService, summaryContextBuilder,
				chatClient, summaryMapper, citationMapper, documentMapper, props, ragMetrics);
	}

	@Test
	void throwsWhenCompanyMissing() {
		when(companyMapper.selectById(99L)).thenReturn(null);
		SummaryGenerateRequest request = new SummaryGenerateRequest();
		request.setCompanyId(99L);
		assertThrows(IllegalArgumentException.class, () -> service.generate(request));
		verify(ragMetrics).markQaFailure();
	}

	@Test
	void generatePersistsSummary() {
		Company company = new Company();
		company.setId(1L);
		company.setName("贵州茅台");
		company.setStockCode("600519");
		when(companyMapper.selectById(1L)).thenReturn(company);

		DocumentChunk chunk = new DocumentChunk();
		chunk.setId(3L);
		chunk.setDocumentId(8L);
		chunk.setContent("经营稳健");
		RetrievedChunk ranked = RetrievedChunk.builder().chunkId(3L).documentId(8L).chunk(chunk).fusedScore(0.7).build();
		when(summaryAggregationService.aggregate(any(), any(), any(), any(), anyInt())).thenReturn(List.of(ranked));
		when(heuristicRerankService.rerank(anyString(), any(), anyInt())).thenReturn(List.of(ranked));
		when(summaryContextBuilder.build(any(), any(), any())).thenReturn(SummaryContext.builder()
				.systemPrompt("sys")
				.userPrompt("user")
				.usedChunks(List.of(ranked))
				.template(SummaryTemplate.of(SummaryMode.FAST))
				.contextChars(50)
				.build());
		when(chatClient.chat(anyString(), anyString())).thenReturn("## 公司概况\n经营稳健 [1]");
		when(documentMapper.selectList(any())).thenReturn(List.of());
		doAnswer(invocation -> {
			Summary summary = invocation.getArgument(0);
			summary.setId(55L);
			return 1;
		}).when(summaryMapper).insert(any(Summary.class));

		SummaryGenerateRequest request = new SummaryGenerateRequest();
		request.setCompanyId(1L);
		request.setMode(SummaryMode.FAST);
		SummaryAnswerVO answer = service.generate(request);

		assertEquals(55L, answer.getSummaryId());
		assertTrue(answer.getTitle().contains("贵州茅台"));
		verify(ragMetrics).recordGenerate(anyLong());
		verify(ragMetrics).markQaSuccess(false);
	}
}
