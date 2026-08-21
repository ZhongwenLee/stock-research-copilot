package com.stockresearch.copilot.rag.context;

import com.stockresearch.copilot.common.enums.IntentType;
import com.stockresearch.copilot.common.enums.SummaryMode;
import com.stockresearch.copilot.config.AppProperties;
import com.stockresearch.copilot.entity.DocumentChunk;
import com.stockresearch.copilot.rag.intent.QuestionIntent;
import com.stockresearch.copilot.rag.retrieve.RetrievedChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummaryContextBuilderTest {

	private SummaryContextBuilder builder;

	@BeforeEach
	void setUp() {
		AppProperties props = new AppProperties();
		props.getQa().setMaxContextChars(3000);
		props.getQa().setRerankTopK(6);
		builder = new SummaryContextBuilder(props);
	}

	@Test
	void buildsFastModeTemplateWithSections() {
		DocumentChunk chunk = new DocumentChunk();
		chunk.setId(1L);
		chunk.setContent("公司主营白酒业务，渠道稳定。");
		chunk.setTitlePath("公司概况");

		SummaryContext context = builder.build(
				QuestionIntent.builder()
						.intentType(IntentType.SUMMARY)
						.companyName("贵州茅台")
						.rawQuestion("请生成公司研究摘要")
						.build(),
				List.of(RetrievedChunk.builder().chunkId(1L).chunk(chunk).build()),
				SummaryMode.FAST);

		assertEquals(SummaryMode.FAST, context.getTemplate().getMode());
		assertTrue(context.getUserPrompt().contains("摘要章节"));
		assertEquals(1, context.getUsedChunks().size());
	}
}
