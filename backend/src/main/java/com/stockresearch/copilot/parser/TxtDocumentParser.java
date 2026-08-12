package com.stockresearch.copilot.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class TxtDocumentParser implements DocumentParser {

	private static final Pattern HEADING = Pattern.compile(
			"^(第[一二三四五六七八九十百千0-9]+[章节部分]|[一二三四五六七八九十]+[、.]|\\d+(\\.\\d+)*\\s+|#{1,6}\\s+).+");

	private final TextCleaner textCleaner;

	@Override
	public boolean supports(String fileExt) {
		return "txt".equalsIgnoreCase(fileExt);
	}

	@Override
	public ParsedDocument parse(Path filePath) {
		try {
			String raw = Files.readString(filePath, StandardCharsets.UTF_8);
			String cleaned = textCleaner.clean(raw);
			List<ParsedSection> sections = splitByHeading(cleaned);
			String title = sections.isEmpty() ? filePath.getFileName().toString()
					: firstNonBlank(sections.get(0).getSection(), filePath.getFileName().toString());
			return ParsedDocument.builder().title(title).sections(sections).build();
		}
		catch (Exception ex) {
			throw new IllegalStateException("failed to parse txt: " + ex.getMessage(), ex);
		}
	}

	List<ParsedSection> splitByHeading(String text) {
		List<ParsedSection> sections = new ArrayList<>();
		String currentTitle = "正文";
		StringBuilder buffer = new StringBuilder();
		for (String line : text.split("\n")) {
			Matcher matcher = HEADING.matcher(line.trim());
			if (matcher.find() && buffer.length() > 0) {
				sections.add(section(currentTitle, buffer.toString()));
				buffer.setLength(0);
				currentTitle = normalizeHeading(line);
			}
			else if (matcher.find()) {
				currentTitle = normalizeHeading(line);
			}
			else {
				if (buffer.length() > 0) {
					buffer.append('\n');
				}
				buffer.append(line);
			}
		}
		if (buffer.length() > 0) {
			sections.add(section(currentTitle, buffer.toString()));
		}
		if (sections.isEmpty() && !text.isBlank()) {
			sections.add(section("正文", text));
		}
		return sections;
	}

	private ParsedSection section(String title, String content) {
		return ParsedSection.builder()
				.titlePath(title)
				.section(title)
				.content(textCleaner.clean(content))
				.build();
	}

	private String normalizeHeading(String line) {
		return line.replaceFirst("^#{1,6}\\s+", "").trim();
	}

	private String firstNonBlank(String preferred, String fallback) {
		return preferred == null || preferred.isBlank() ? fallback : preferred;
	}
}
