package com.stockresearch.copilot.service.impl;

import com.stockresearch.copilot.common.enums.IntentType;
import com.stockresearch.copilot.common.metrics.RagMetrics;
import com.stockresearch.copilot.config.AppProperties;
import com.stockresearch.copilot.dto.QaAskRequest;
import com.stockresearch.copilot.entity.DocumentChunk;
import com.stockresearch.copilot.entity.Question;
import com.stockresearch.copilot.mapper.CitationMapper;
import com.stockresearch.copilot.mapper.CompanyMapper;
import com.stockresearch.copilot.mapper.DocumentMapper;
import com.stockresearch.copilot.mapper.QuestionMapper;
import com.stockresearch.copilot.rag.context.ContextBuilder;
import com.stockresearch.copilot.rag.context.PromptContext;
import com.stockresearch.copilot.rag.intent.IntentRecognizer;
import com.stockresearch.copilot.rag.intent.QuestionIntent;
import com.stockresearch.copilot.rag.llm.ChatClient;
import com.stockresearch.copilot.rag.rerank.HeuristicRerankService;
import com.stockresearch.copilot.rag.retrieve.HybridRetrievalService;
import com.stockresearch.copilot.rag.retrieve.RetrievedChunk;
import com.stockresearch.copilot.vo.QaAnswerVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QaServiceImplTest {

	@Mock private IntentRecognizer intentRecognizer;
	@Mock private HybridRetrievalService hybridRetrievalService;
	@Mock private HeuristicRerankService heuristicRerankService;
	@Mock private ContextBuilder contextBuilder;
	@Mock private ChatClient chatClient;
	@Mock private QuestionMapper questionMapper;
	@Mock private CitationMapper citationMapper;
	@Mock private CompanyMapper companyMapper;
	@Mock private DocumentMapper documentMapper;
	@Mock private RagMetrics ragMetrics;

	private QaServiceImpl service;

	@BeforeEach
	void setUp() {
		AppProperties props = new AppProperties();
		props.getQa().setRecallTopK(10);
		props.getQa().setRerankTopK(5);
		props.getQa().setQuoteMaxChars(120);
		service = new QaServiceImpl(
				intentRecognizer, hybridRetrievalService, heuristicRerankService, contextBuilder,
				chatClient, questionMapper, citationMapper, companyMapper, documentMapper, props, ragMetrics);
	}

	@Test
	void askPersistsAnswerAndCitations() {
		QuestionIntent intent = QuestionIntent.builder()
				.intentType(IntentType.QA)
				.companyId(1L)
				.companyName("贵州茅台")
				.stockCode("600519")
				.rawQuestion("营收如何")
				.preferredDocTypes(List.of())
				.build();
		when(intentRecognizer.recognize(anyString(), any(), any(), any())).thenReturn(intent);

		DocumentChunk chunk = new DocumentChunk();
		chunk.setId(11L);
		chunk.setDocumentId(21L);
		chunk.setContent("营收同比增长 12%");
		RetrievedChunk ranked = RetrievedChunk.builder()
				.chunkId(11L).documentId(21L).chunk(chunk).fusedScore(0.8).build();
		when(hybridRetrievalService.retrieve(any(), anyInt())).thenReturn(List.of(ranked));
		when(heuristicRerankService.rerank(anyString(), any(), anyInt())).thenReturn(List.of(ranked));
		when(contextBuilder.build(any(), any())).thenReturn(PromptContext.builder()
				.systemPrompt("sys")
				.userPrompt("user")
				.usedChunks(List.of(ranked))
				.contextChars(100)
				.build());
		when(chatClient.chat(anyString(), anyString())).thenReturn("营收增长良好 [1]");
		when(documentMapper.selectList(any())).thenReturn(List.of());
		doAnswer(invocation -> {
			Question q = invocation.getArgument(0);
			q.setId(99L);
			return 1;
		}).when(questionMapper).insert(any(Question.class));

		QaAskRequest request = new QaAskRequest();
		request.setQuestion("营收如何");
		request.setCompanyId(1L);
		QaAnswerVO answer = service.ask(request);

		assertEquals(99L, answer.getQuestionId());
		assertEquals("营收增长良好 [1]", answer.getAnswer());
		assertFalse(answer.isInsufficientEvidence());
		assertEquals(1, answer.getCitations().size());
		verify(ragMetrics).markQaSuccess(false);
		verify(ragMetrics).recordGenerate(anyLong());
	}

	@Test
	void marksInsufficientWhenNoEvidence() {
		QuestionIntent intent = QuestionIntent.builder()
				.intentType(IntentType.QA)
				.rawQuestion("营收如何")
				.preferredDocTypes(List.of())
				.build();
		when(intentRecognizer.recognize(anyString(), any(), any(), any())).thenReturn(intent);
		when(hybridRetrievalService.retrieve(any(), anyInt())).thenReturn(List.of());
		when(heuristicRerankService.rerank(anyString(), any(), anyInt())).thenReturn(List.of());
		when(contextBuilder.build(any(), any())).thenReturn(PromptContext.builder()
				.systemPrompt("sys")
				.userPrompt("user")
				.usedChunks(List.of())
				.contextChars(0)
				.build());
		when(chatClient.chat(anyString(), anyString())).thenReturn("依据不足，请补充文档");
		doAnswer(invocation -> {
			Question q = invocation.getArgument(0);
			q.setId(1L);
			return 1;
		}).when(questionMapper).insert(any(Question.class));

		QaAskRequest request = new QaAskRequest();
		request.setQuestion("营收如何");
		QaAnswerVO answer = service.ask(request);

		assertTrue(answer.isInsufficientEvidence());
		verify(ragMetrics).markQaSuccess(true);
	}
}
