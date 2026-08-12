package com.stockresearch.copilot.parser;

import com.stockresearch.copilot.common.exception.BizException;
import com.stockresearch.copilot.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@Component
public class DocumentParserRegistry {

	private final List<DocumentParser> parsers;

	public DocumentParserRegistry(List<DocumentParser> parsers) {
		this.parsers = parsers;
	}

	public ParsedDocument parse(Path filePath, String fileExt) {
		String ext = fileExt == null ? "" : fileExt.toLowerCase(Locale.ROOT);
		return parsers.stream()
				.filter(parser -> parser.supports(ext))
				.findFirst()
				.orElseThrow(() -> new BizException(ErrorCode.VALIDATION_FAILED, "unsupported file type: " + ext))
				.parse(filePath);
	}
}
