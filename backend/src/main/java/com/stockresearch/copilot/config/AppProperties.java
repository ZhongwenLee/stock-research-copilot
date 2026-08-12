package com.stockresearch.copilot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

	private FileStorage fileStorage = new FileStorage();
	private Ingest ingest = new Ingest();
	private Ai ai = new Ai();
	private Vector vector = new Vector();
	private Milvus milvus = new Milvus();

	@Data
	public static class FileStorage {
		private String path = "./data/uploads";
		private long maxFileSizeBytes = 50L * 1024 * 1024;
		private List<String> allowedExtensions = new ArrayList<>(List.of("pdf", "docx", "txt", "html", "htm"));
	}

	@Data
	public static class Ingest {
		private boolean asyncEnabled = true;
		private int chunkMaxChars = 800;
		private int chunkOverlapChars = 100;
		private int embeddingBatchSize = 16;
		private int embeddingMaxRetries = 3;
	}

	@Data
	public static class Ai {
		private String apiKey = "";
		private String baseUrl = "https://api.openai.com/v1";
		private String chatModel = "gpt-4o-mini";
		private String embeddingModel = "text-embedding-3-small";
		private int embeddingDimensions = 1536;
	}

	@Data
	public static class Vector {
		/** memory | milvus */
		private String provider = "memory";
	}

	@Data
	public static class Milvus {
		private boolean enabled = false;
		private String host = "127.0.0.1";
		private int port = 19530;
		private String collection = "document_chunks";
	}
}
