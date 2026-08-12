package com.stockresearch.copilot.config;

import com.stockresearch.copilot.rag.embedding.EmbeddingClient;
import com.stockresearch.copilot.rag.embedding.OpenAiCompatibleEmbeddingClient;
import com.stockresearch.copilot.rag.embedding.StubEmbeddingClient;
import com.stockresearch.copilot.rag.vector.InMemoryVectorStore;
import com.stockresearch.copilot.rag.vector.MilvusVectorStore;
import com.stockresearch.copilot.rag.vector.VectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RagConfig {

	private final AppProperties appProperties;

	@Bean
	public EmbeddingClient embeddingClient() {
		if (!StringUtils.hasText(appProperties.getAi().getApiKey())) {
			log.warn("AI_API_KEY is empty, using StubEmbeddingClient");
			return new StubEmbeddingClient(appProperties);
		}
		RestClient restClient = RestClient.builder()
				.baseUrl(trimTrailingSlash(appProperties.getAi().getBaseUrl()))
				.defaultHeader("Authorization", "Bearer " + appProperties.getAi().getApiKey())
				.requestFactory(requestFactory())
				.build();
		log.info("using OpenAiCompatibleEmbeddingClient model={}", appProperties.getAi().getEmbeddingModel());
		return new OpenAiCompatibleEmbeddingClient(appProperties, restClient);
	}

	@Bean
	public VectorStore vectorStore(EmbeddingClient embeddingClient) {
		String provider = appProperties.getVector().getProvider() == null
				? "memory"
				: appProperties.getVector().getProvider().trim().toLowerCase();
		if ("milvus".equals(provider) && appProperties.getMilvus().isEnabled()) {
			log.info("using MilvusVectorStore {}:{}", appProperties.getMilvus().getHost(),
					appProperties.getMilvus().getPort());
			return new MilvusVectorStore(appProperties, embeddingClient.dimensions());
		}
		log.info("using InMemoryVectorStore (provider={})", provider);
		return new InMemoryVectorStore();
	}

	private JdkClientHttpRequestFactory requestFactory() {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
		factory.setReadTimeout(Duration.ofSeconds(60));
		return factory;
	}

	private String trimTrailingSlash(String baseUrl) {
		if (baseUrl == null || baseUrl.isBlank()) {
			return "https://api.openai.com/v1";
		}
		return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
	}
}
