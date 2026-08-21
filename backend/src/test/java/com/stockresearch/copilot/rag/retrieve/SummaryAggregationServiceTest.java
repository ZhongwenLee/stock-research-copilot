package com.stockresearch.copilot.rag.retrieve;

import com.stockresearch.copilot.common.enums.IntentType;
import com.stockresearch.copilot.entity.Document;
import com.stockresearch.copilot.entity.DocumentChunk;
import com.stockresearch.copilot.mapper.DocumentChunkMapper;
import com.stockresearch.copilot.mapper.DocumentMapper;
import com.stockresearch.copilot.rag.intent.QuestionIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryAggregationServiceTest {

	@Mock
	private DocumentMapper documentMapper;
	@Mock
	private DocumentChunkMapper documentChunkMapper;

	private SummaryAggregationService service;

	@BeforeEach
	void setUp() {
		service = new SummaryAggregationService(documentMapper, documentChunkMapper);
	}

	@Test
	void returnsEmptyWhenNoReadyDocuments() {
		QuestionIntent intent = QuestionIntent.builder()
				.intentType(IntentType.SUMMARY)
				.companyId(7L)
				.rawQuestion("请生成公司研究摘要")
				.build();
		when(documentMapper.selectList(any())).thenReturn(List.of());

		assertTrue(service.aggregate(intent, null, null, null, 10).isEmpty());
	}

	@Test
	void aggregatesAndRanksChunks() {
		Document document = new Document();
		document.setId(10L);
		document.setDocType("FINANCIAL_REPORT");
		when(documentMapper.selectList(any())).thenReturn(List.of(document));

		DocumentChunk chunk = new DocumentChunk();
		chunk.setId(1L);
		chunk.setDocumentId(10L);
		chunk.setCompanyId(7L);
		chunk.setContent("请生成公司研究摘要 相关经营情况");
		chunk.setTitlePath("经营情况");
		chunk.setSection("经营情况");
		chunk.setChunkIndex(0);
		when(documentChunkMapper.selectList(any())).thenReturn(List.of(chunk));

		List<RetrievedChunk> result = service.aggregate(
				QuestionIntent.builder()
						.intentType(IntentType.SUMMARY)
						.companyId(7L)
						.rawQuestion("请生成公司研究摘要")
						.build(),
				null, null, null, 5);

		assertEquals(1, result.size());
		assertEquals(1L, result.get(0).getChunkId());
		assertEquals("aggregate", result.get(0).getSource());
	}
}
