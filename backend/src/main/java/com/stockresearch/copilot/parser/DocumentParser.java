package com.stockresearch.copilot.parser;

import java.nio.file.Path;

public interface DocumentParser {

	boolean supports(String fileExt);

	ParsedDocument parse(Path filePath);
}
