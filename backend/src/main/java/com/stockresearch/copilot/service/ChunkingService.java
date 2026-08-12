package com.stockresearch.copilot.service;

import com.stockresearch.copilot.config.AppProperties;
import com.stockresearch.copilot.parser.ChunkDraft;
import com.stockresearch.copilot.parser.ParsedDocument;
import com.stockresearch.copilot.parser.ParsedSection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Heading-first chunking with length-based secondary split.
 */
@Service
@RequiredArgsConstructor
public class ChunkingService {

	private final AppProperties appProperties;

	public List<ChunkDraft> chunk(ParsedDocument parsedDocument) {
		int maxChars = Math.max(100, appProperties.getIngest().getChunkMaxChars());
		int overlap = Math.max(0, Math.min(appProperties.getIngest().getChunkOverlapChars(), maxChars / 2));

		List<ChunkDraft> drafts = new ArrayList<>();
		int index = 0;
		for (ParsedSection section : parsedDocument.getSections()) {
			String content = section.getContent() == null ? "" : section.getContent().trim();
			if (content.isBlank()) {
				continue;
			}
			List<String> pieces = splitByLength(content, maxChars, overlap);
			for (String piece : pieces) {
				drafts.add(ChunkDraft.builder()
						.chunkIndex(index++)
						.titlePath(section.getTitlePath())
						.section(section.getSection())
						.content(piece)
						.pageNo(section.getPageNo())
						.tokenCount(estimateTokenCount(piece))
						.build());
			}
		}
		return drafts;
	}

	private List<String> splitByLength(String text, int maxChars, int overlap) {
		if (text.length() <= maxChars) {
			return List.of(text);
		}
		List<String> pieces = new ArrayList<>();
		int start = 0;
		while (start < text.length()) {
			int end = Math.min(text.length(), start + maxChars);
			if (end < text.length()) {
				int breakAt = findBreak(text, start, end);
				if (breakAt > start + maxChars / 3) {
					end = breakAt;
				}
			}
			pieces.add(text.substring(start, end).trim());
			if (end >= text.length()) {
				break;
			}
			start = Math.max(end - overlap, start + 1);
		}
		return pieces.stream().filter(piece -> !piece.isBlank()).toList();
	}

	private int findBreak(String text, int start, int end) {
		for (int i = end; i > start; i--) {
			char c = text.charAt(i - 1);
			if (c == '\n' || c == '。' || c == '！' || c == '？' || c == '.' || c == '!' || c == '?') {
				return i;
			}
		}
		for (int i = end; i > start; i--) {
			if (Character.isWhitespace(text.charAt(i - 1))) {
				return i;
			}
		}
		return end;
	}

	private int estimateTokenCount(String text) {
		// Rough heuristic: CJK ~1 token/char, latin ~1 token/4 chars.
		int cjk = 0;
		int other = 0;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
				cjk++;
			}
			else if (!Character.isWhitespace(c)) {
				other++;
			}
		}
		return Math.max(1, cjk + (other + 3) / 4);
	}
}
