package com.stockresearch.copilot.rag.llm;

public interface ChatClient {

	String chat(String systemPrompt, String userPrompt);
}
