package com.stockresearch.copilot.service;

import com.stockresearch.copilot.config.AppProperties;
import com.stockresearch.copilot.parser.ChunkDraft;
import com.stockresearch.copilot.parser.ParsedDocument;
import com.stockresearch.copilot.parser.ParsedSection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkingServiceTest {

	@Test
	void shouldSplitLongSectionByLength() {
		AppProperties properties = new AppProperties();
		properties.getIngest().setChunkMaxChars(120);
		properties.getIngest().setChunkOverlapChars(20);
		ChunkingService service = new ChunkingService(properties);

		String content = ("risk-factor-paragraph ").repeat(40);
		ParsedDocument parsed = ParsedDocument.builder()
				.title("测试")
				.sections(List.of(ParsedSection.builder()
						.titlePath("风险因素")
						.section("风险因素")
						.content(content)
						.pageNo(1)
						.build()))
				.build();

		List<ChunkDraft> drafts = service.chunk(parsed);
		assertFalse(drafts.isEmpty());
		assertTrue(drafts.size() > 1);
		assertTrue(drafts.stream().allMatch(item -> item.getContent().length() <= 250));
	}
}
