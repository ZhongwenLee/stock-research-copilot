package com.stockresearch.copilot.rag.context;

import com.stockresearch.copilot.rag.retrieve.RetrievedChunk;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SummaryContext {

	private SummaryTemplate template;
	private String systemPrompt;
	private String userPrompt;
	private List<RetrievedChunk> usedChunks;
	private int contextChars;
}
