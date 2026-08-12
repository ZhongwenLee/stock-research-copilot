package com.stockresearch.copilot.parser;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TxtDocumentParserTest {

	@Test
	void shouldParseHeadings() throws Exception {
		Path file = Files.createTempFile("sample", ".txt");
		Files.writeString(file, """
				一、公司概况
				本公司主要从事白酒生产与销售。

				二、风险因素
				市场竞争加剧可能影响业绩。
				""", StandardCharsets.UTF_8);

		TxtDocumentParser parser = new TxtDocumentParser(new TextCleaner());
		ParsedDocument parsed = parser.parse(file);
		assertFalse(parsed.getSections().isEmpty());
		assertTrue(parsed.getSections().stream().anyMatch(s -> s.getSection().contains("风险")));
		Files.deleteIfExists(file);
	}
}
