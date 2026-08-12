package com.stockresearch.copilot.parser;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class TextCleaner {

	private static final Pattern MULTI_BLANK = Pattern.compile("[ \\t\\x0B\\f\\r]+");
	private static final Pattern MULTI_NEWLINE = Pattern.compile("\\n{3,}");
	private static final Pattern HEADER_FOOTER = Pattern.compile("(?m)^\\s*(第?\\s*\\d+\\s*页|Page\\s+\\d+(\\s+of\\s+\\d+)?)\\s*$");

	public String clean(String raw) {
		if (raw == null || raw.isBlank()) {
			return "";
		}
		String text = raw.replace('\u00A0', ' ');
		text = text.replace("\r\n", "\n").replace('\r', '\n');
		text = HEADER_FOOTER.matcher(text).replaceAll("");
		text = Arrays.stream(text.split("\n"))
				.map(String::trim)
				.filter(line -> !line.isEmpty())
				.collect(Collectors.joining("\n"));
		text = MULTI_BLANK.matcher(text).replaceAll(" ");
		text = MULTI_NEWLINE.matcher(text).replaceAll("\n\n");
		return text.trim();
	}

	public List<String> splitParagraphs(String text) {
		if (text == null || text.isBlank()) {
			return List.of();
		}
		return Arrays.stream(text.split("\\n\\s*\\n|\\n"))
				.map(String::trim)
				.filter(line -> !line.isEmpty())
				.toList();
	}
}
