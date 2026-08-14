package com.stockresearch.copilot.rag.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.stockresearch.copilot.common.exception.BizException;
import com.stockresearch.copilot.common.exception.ErrorCode;
import com.stockresearch.copilot.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class OpenAiCompatibleChatClient implements ChatClient {

	private final AppProperties appProperties;
	private final RestClient restClient;

	@Override
	public String chat(String systemPrompt, String userPrompt) {
		Map<String, Object> body = new HashMap<>();
		body.put("model", appProperties.getAi().getChatModel());
		body.put("temperature", 0.2);
		body.put("messages", List.of(
				Map.of("role", "system", "content", systemPrompt == null ? "" : systemPrompt),
				Map.of("role", "user", "content", userPrompt == null ? "" : userPrompt)));

		try {
			ChatResponse response = restClient.post()
					.uri("/chat/completions")
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.retrieve()
					.body(ChatResponse.class);
			if (response == null || response.choices == null || response.choices.isEmpty()
					|| response.choices.get(0).message == null
					|| response.choices.get(0).message.content == null
					|| response.choices.get(0).message.content.isBlank()) {
				throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "empty chat response");
			}
			return response.choices.get(0).message.content.trim();
		}
		catch (BizException ex) {
			throw ex;
		}
		catch (Exception ex) {
			log.error("chat completion failed", ex);
			throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "chat completion failed: " + ex.getMessage());
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class ChatResponse {
		public List<Choice> choices;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class Choice {
		public Message message;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class Message {
		public String content;
	}
}
