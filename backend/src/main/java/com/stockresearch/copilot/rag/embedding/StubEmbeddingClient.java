package com.stockresearch.copilot.rag.embedding;

import com.stockresearch.copilot.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic local embedding for offline / no-API-key development.
 */
@Slf4j
@RequiredArgsConstructor
public class StubEmbeddingClient implements EmbeddingClient {

	private final AppProperties appProperties;

	@Override
	public List<float[]> embed(List<String> texts) {
		List<float[]> vectors = new ArrayList<>(texts.size());
		for (String text : texts) {
			vectors.add(hashToVector(text == null ? "" : text));
		}
		return vectors;
	}

	@Override
	public int dimensions() {
		return appProperties.getAi().getEmbeddingDimensions();
	}

	private float[] hashToVector(String text) {
		int dim = dimensions();
		float[] vector = new float[dim];
		byte[] seed = sha256(text);
		for (int i = 0; i < dim; i++) {
			int b = seed[i % seed.length] & 0xff;
			int b2 = seed[(i * 7 + 3) % seed.length] & 0xff;
			vector[i] = ((b / 255.0f) * 2 - 1f) * 0.7f + ((b2 / 255.0f) * 2 - 1f) * 0.3f;
		}
		normalize(vector);
		return vector;
	}

	private byte[] sha256(String text) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private void normalize(float[] vector) {
		double sum = 0;
		for (float v : vector) {
			sum += v * v;
		}
		double norm = Math.sqrt(sum);
		if (norm < 1e-8) {
			return;
		}
		for (int i = 0; i < vector.length; i++) {
			vector[i] = (float) (vector[i] / norm);
		}
	}
}
