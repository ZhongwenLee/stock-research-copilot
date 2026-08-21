package com.stockresearch.copilot.rag.context;

import com.stockresearch.copilot.common.enums.IntentType;
import com.stockresearch.copilot.config.AppProperties;
import com.stockresearch.copilot.entity.DocumentChunk;
import com.stockresearch.copilot.rag.intent.QuestionIntent;
import com.stockresearch.copilot.rag.retrieve.RetrievedChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextBuilderTest {

	private ContextBuilder builder;

	@BeforeEach
	void setUp() {
		AppProperties props = new AppProperties();
		props.getQa().setMaxContextChars(2000);
		builder = new ContextBuilder(props);
	}

	@Test
	void buildsPromptsWithCitationsWhenChunksExist() {
		DocumentChunk chunk = new DocumentChunk();
		chunk.setId(1L);
		chunk.setContent("2024 年营收同比增长 15%。");
		chunk.setTitlePath("经营情况");
		chunk.setPageNo(3);

		PromptContext context = builder.build(
				QuestionIntent.builder()
						.intentType(IntentType.QA)
						.companyName("贵州茅台")
						.stockCode("600519")
						.rawQuestion("营收如何")
						.build(),
				List.of(RetrievedChunk.builder().chunkId(1L).chunk(chunk).build()));

		assertFalse(context.getUsedChunks().isEmpty());
		assertTrue(context.getUserPrompt().contains("参考资料"));
		assertTrue(context.getUserPrompt().contains("[1]"));
		assertTrue(context.getSystemPrompt().contains("依据不足"));
	}

	@Test
	void asksForInsufficientEvidenceWhenNoChunks() {
		PromptContext context = builder.build(
				QuestionIntent.builder()
						.intentType(IntentType.QA)
						.rawQuestion("营收如何")
						.build(),
				List.of());

		assertTrue(context.getUsedChunks().isEmpty());
		assertTrue(context.getUserPrompt().contains("依据不足"));
	}
}
