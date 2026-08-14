package com.stockresearch.copilot.rag.rerank;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stockresearch.copilot.entity.DocumentChunk;
import com.stockresearch.copilot.mapper.DocumentChunkMapper;
import com.stockresearch.copilot.rag.retrieve.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Heuristic reranker for v1. Cloud Rerank API can replace this later.
 */
@Service
@RequiredArgsConstructor
public class HeuristicRerankService {

	private final DocumentChunkMapper documentChunkMapper;

	public List<RetrievedChunk> rerank(String question, List<RetrievedChunk> candidates, int topK) {
		if (candidates == null || candidates.isEmpty() || topK <= 0) {
			return List.of();
		}
		enrichChunks(candidates);
		String q = question == null ? "" : question.toLowerCase(Locale.ROOT);
		List<RetrievedChunk> scored = new ArrayList<>(candidates.size());
		for (RetrievedChunk item : candidates) {
			DocumentChunk chunk = item.getChunk();
			if (chunk == null || !StringUtils.hasText(chunk.getContent())) {
				continue;
			}
			double score = item.getFusedScore() * 0.55
					+ item.getVectorScore() * 0.25
					+ item.getKeywordScore() * 0.15
					+ lexicalOverlap(q, chunk) * 0.05;
			if (StringUtils.hasText(chunk.getTitlePath()) && overlaps(q, chunk.getTitlePath().toLowerCase(Locale.ROOT))) {
				score += 0.03;
			}
			item.setFusedScore(score);
			scored.add(item);
		}
		scored.sort(Comparator.comparingDouble(RetrievedChunk::getFusedScore).reversed());
		if (scored.size() > topK) {
			return new ArrayList<>(scored.subList(0, topK));
		}
		return scored;
	}

	private void enrichChunks(List<RetrievedChunk> candidates) {
		List<Long> missingIds = candidates.stream()
				.filter(item -> item.getChunk() == null && item.getChunkId() != null)
				.map(RetrievedChunk::getChunkId)
				.distinct()
				.toList();
		if (missingIds.isEmpty()) {
			return;
		}
		Map<Long, DocumentChunk> map = new HashMap<>();
		documentChunkMapper.selectList(new LambdaQueryWrapper<DocumentChunk>()
						.in(DocumentChunk::getId, missingIds))
				.forEach(chunk -> map.put(chunk.getId(), chunk));
		for (RetrievedChunk item : candidates) {
			if (item.getChunk() == null) {
				item.setChunk(map.get(item.getChunkId()));
			}
		}
	}

	private double lexicalOverlap(String question, DocumentChunk chunk) {
		String content = chunk.getContent().toLowerCase(Locale.ROOT);
		int hits = 0;
		int total = 0;
		for (String token : question.split("[\\s，。！？；：、]+")) {
			String t = token.trim();
			if (t.length() < 2) {
				continue;
			}
			total++;
			if (content.contains(t)) {
				hits++;
			}
		}
		return total == 0 ? 0 : (double) hits / total;
	}

	private boolean overlaps(String question, String title) {
		for (String token : question.split("[\\s，。！？；：、]+")) {
			String t = token.trim();
			if (t.length() >= 2 && title.contains(t)) {
				return true;
			}
		}
		return false;
	}
}
