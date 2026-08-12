package com.stockresearch.copilot.rag.embedding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.stockresearch.copilot.common.exception.BizException;
import com.stockresearch.copilot.common.exception.ErrorCode;
import com.stockresearch.copilot.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class OpenAiCompatibleEmbeddingClient implements EmbeddingClient {

	private final AppProperties appProperties;
	private final RestClient restClient;

	@Override
	public List<float[]> embed(List<String> texts) {
		if (texts == null || texts.isEmpty()) {
			return List.of();
		}
		int retries = Math.max(1, appProperties.getIngest().getEmbeddingMaxRetries());
		RuntimeException last = null;
		for (int attempt = 1; attempt <= retries; attempt++) {
			try {
				return doEmbed(texts);
			}
			catch (RuntimeException ex) {
				last = ex;
				log.warn("embedding attempt {}/{} failed: {}", attempt, retries, ex.getMessage());
				sleepQuietly(200L * attempt);
			}
		}
		throw new BizException(ErrorCode.SERVICE_UNAVAILABLE,
				"embedding failed after retries: " + (last == null ? "unknown" : last.getMessage()));
	}

	@Override
	public int dimensions() {
		return appProperties.getAi().getEmbeddingDimensions();
	}

	private List<float[]> doEmbed(List<String> texts) {
		Map<String, Object> body = new HashMap<>();
		body.put("model", appProperties.getAi().getEmbeddingModel());
		body.put("input", texts);

		EmbeddingResponse response = restClient.post()
				.uri("/embeddings")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(EmbeddingResponse.class);

		if (response == null || response.data == null || response.data.isEmpty()) {
			throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "empty embedding response");
		}
		List<float[]> vectors = new ArrayList<>(texts.size());
		response.data.stream()
				.sorted((a, b) -> Integer.compare(a.index == null ? 0 : a.index, b.index == null ? 0 : b.index))
				.forEach(item -> vectors.add(toFloatArray(item.embedding)));
		if (vectors.size() != texts.size()) {
			throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "embedding size mismatch");
		}
		return vectors;
	}

	private float[] toFloatArray(JsonNode node) {
		if (node == null || !node.isArray()) {
			throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "invalid embedding vector");
		}
		float[] values = new float[node.size()];
		for (int i = 0; i < node.size(); i++) {
			values[i] = (float) node.get(i).asDouble();
		}
		return values;
	}

	private void sleepQuietly(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class EmbeddingResponse {
		public List<EmbeddingData> data;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class EmbeddingData {
		public Integer index;
		public JsonNode embedding;
	}
}
