package com.stockresearch.copilot.parser;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ParsedDocument {

	private String title;

	@Builder.Default
	private List<ParsedSection> sections = new ArrayList<>();
}
