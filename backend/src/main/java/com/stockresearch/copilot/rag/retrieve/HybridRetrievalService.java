package com.stockresearch.copilot.rag.retrieve;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stockresearch.copilot.common.enums.DocType;
import com.stockresearch.copilot.common.enums.ProcessStatus;
import com.stockresearch.copilot.entity.Document;
import com.stockresearch.copilot.entity.DocumentChunk;
import com.stockresearch.copilot.mapper.DocumentChunkMapper;
import com.stockresearch.copilot.mapper.DocumentMapper;
import com.stockresearch.copilot.rag.embedding.EmbeddingClient;
import com.stockresearch.copilot.rag.intent.QuestionIntent;
import com.stockresearch.copilot.rag.vector.VectorSearchFilter;
import com.stockresearch.copilot.rag.vector.VectorSearchHit;
import com.stockresearch.copilot.rag.vector.VectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRetrievalService {

	private static final Pattern TOKEN_SPLIT = Pattern.compile("[\\s,，。！？；;：:、\\[\\]（）()【】\"“”'‘’/\\\\|]+");

	private final EmbeddingClient embeddingClient;
	private final VectorStore vectorStore;
	private final DocumentMapper documentMapper;
	private final DocumentChunkMapper documentChunkMapper;

	public List<RetrievedChunk> retrieve(QuestionIntent intent, int recallTopK) {
		Set<Long> documentIds = resolveDocumentIds(intent);
		if (intent.getCompanyId() != null && documentIds.isEmpty()) {
			log.info("no READY documents for companyId={}", intent.getCompanyId());
			return List.of();
		}

		List<RetrievedChunk> vectorHits = vectorRecall(intent, documentIds, recallTopK);
		List<RetrievedChunk> keywordHits = keywordRecall(intent, documentIds, recallTopK);
		return fuse(vectorHits, keywordHits, recallTopK);
	}

	private Set<Long> resolveDocumentIds(QuestionIntent intent) {
		LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<Document>()
				.eq(Document::getProcessStatus, ProcessStatus.READY.name());
		if (intent.getCompanyId() != null) {
			wrapper.eq(Document::getCompanyId, intent.getCompanyId());
		}
		if (intent.getPreferredDocTypes() != null && !intent.getPreferredDocTypes().isEmpty()) {
			wrapper.in(Document::getDocType, intent.getPreferredDocTypes().stream().map(DocType::name).toList());
		}
		return documentMapper.selectList(wrapper).stream()
				.map(Document::getId)
				.collect(Collectors.toCollection(HashSet::new));
	}

	private List<RetrievedChunk> vectorRecall(QuestionIntent intent, Set<Long> documentIds, int topK) {
		List<float[]> embeddings = embeddingClient.embed(List.of(intent.getRawQuestion()));
		if (embeddings.isEmpty()) {
			return List.of();
		}
		VectorSearchFilter.VectorSearchFilterBuilder filterBuilder = VectorSearchFilter.builder()
				.companyId(intent.getCompanyId());
		if (!documentIds.isEmpty()) {
			filterBuilder.documentIds(documentIds);
		}
		List<VectorSearchHit> hits = vectorStore.search(embeddings.get(0), filterBuilder.build(), topK);
		List<RetrievedChunk> result = new ArrayList<>();
		for (VectorSearchHit hit : hits) {
			result.add(RetrievedChunk.builder()
					.chunkId(hit.getChunkId())
					.documentId(hit.getDocumentId())
					.companyId(hit.getCompanyId())
					.vectorScore(hit.getScore())
					.keywordScore(0)
					.fusedScore(hit.getScore())
					.source("vector")
					.build());
		}
		return result;
	}

	private List<RetrievedChunk> keywordRecall(QuestionIntent intent, Set<Long> documentIds, int topK) {
		List<String> terms = extractTerms(intent.getRawQuestion());
		if (terms.isEmpty()) {
			return List.of();
		}

		LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<>();
		if (intent.getCompanyId() != null) {
			wrapper.eq(DocumentChunk::getCompanyId, intent.getCompanyId());
		}
		if (!documentIds.isEmpty()) {
			wrapper.in(DocumentChunk::getDocumentId, documentIds);
		}
		wrapper.and(w -> {
			boolean first = true;
			for (String term : terms) {
				if (first) {
					w.like(DocumentChunk::getContent, term);
					first = false;
				}
				else {
					w.or().like(DocumentChunk::getContent, term);
				}
			}
			w.or().like(DocumentChunk::getTitlePath, terms.get(0));
		});
		wrapper.last("LIMIT " + Math.max(topK * 3, 30));

		List<DocumentChunk> chunks = documentChunkMapper.selectList(wrapper);
		List<RetrievedChunk> scored = new ArrayList<>();
		for (DocumentChunk chunk : chunks) {
			double score = keywordScore(chunk, terms);
			if (score <= 0) {
				continue;
			}
			scored.add(RetrievedChunk.builder()
					.chunkId(chunk.getId())
					.documentId(chunk.getDocumentId())
					.companyId(chunk.getCompanyId())
					.chunk(chunk)
					.vectorScore(0)
					.keywordScore(score)
					.fusedScore(score)
					.source("keyword")
					.build());
		}
		scored.sort(Comparator.comparingDouble(RetrievedChunk::getKeywordScore).reversed());
		if (scored.size() > topK) {
			return new ArrayList<>(scored.subList(0, topK));
		}
		return scored;
	}

	private List<RetrievedChunk> fuse(List<RetrievedChunk> vectorHits, List<RetrievedChunk> keywordHits, int topK) {
		Map<Long, RetrievedChunk> merged = new LinkedHashMap<>();
		Map<Long, Integer> vectorRank = new HashMap<>();
		Map<Long, Integer> keywordRank = new HashMap<>();

		for (int i = 0; i < vectorHits.size(); i++) {
			RetrievedChunk hit = vectorHits.get(i);
			vectorRank.put(hit.getChunkId(), i + 1);
			merged.put(hit.getChunkId(), hit);
		}
		for (int i = 0; i < keywordHits.size(); i++) {
			RetrievedChunk hit = keywordHits.get(i);
			keywordRank.put(hit.getChunkId(), i + 1);
			RetrievedChunk existing = merged.get(hit.getChunkId());
			if (existing == null) {
				merged.put(hit.getChunkId(), hit);
			}
			else {
				existing.setKeywordScore(Math.max(existing.getKeywordScore(), hit.getKeywordScore()));
				existing.setSource("hybrid");
				if (existing.getChunk() == null) {
					existing.setChunk(hit.getChunk());
				}
			}
		}

		int rrfK = 60;
		List<RetrievedChunk> fused = new ArrayList<>();
		for (RetrievedChunk item : merged.values()) {
			double score = 0;
			Integer vr = vectorRank.get(item.getChunkId());
			Integer kr = keywordRank.get(item.getChunkId());
			if (vr != null) {
				score += 1.0 / (rrfK + vr);
			}
			if (kr != null) {
				score += 1.0 / (rrfK + kr);
			}
			item.setFusedScore(score);
			if ("vector".equals(item.getSource()) && kr != null) {
				item.setSource("hybrid");
			}
			if ("keyword".equals(item.getSource()) && vr != null) {
				item.setSource("hybrid");
			}
			fused.add(item);
		}
		fused.sort(Comparator.comparingDouble(RetrievedChunk::getFusedScore).reversed());
		if (fused.size() > topK) {
			return new ArrayList<>(fused.subList(0, topK));
		}
		return fused;
	}

	List<String> extractTerms(String question) {
		if (!StringUtils.hasText(question)) {
			return List.of();
		}
		String[] parts = TOKEN_SPLIT.split(question);
		LinkedHashSetLike set = new LinkedHashSetLike();
		for (String part : parts) {
			String term = part.trim().toLowerCase(Locale.ROOT);
			if (term.length() < 2) {
				continue;
			}
			if (isStopWord(term)) {
				continue;
			}
			set.add(term);
		}
		// also keep short Chinese bigrams from continuous CJK if term list is thin
		if (set.values.size() < 3) {
			String compact = question.replaceAll("\\s+", "");
			for (int i = 0; i < compact.length() - 1 && set.values.size() < 8; i++) {
				char a = compact.charAt(i);
				char b = compact.charAt(i + 1);
				if (isCjk(a) && isCjk(b)) {
					set.add(String.valueOf(new char[] {a, b}));
				}
			}
		}
		return set.values;
	}

	private double keywordScore(DocumentChunk chunk, List<String> terms) {
		String content = chunk.getContent() == null ? "" : chunk.getContent().toLowerCase(Locale.ROOT);
		String title = chunk.getTitlePath() == null ? "" : chunk.getTitlePath().toLowerCase(Locale.ROOT);
		double score = 0;
		for (String term : terms) {
			if (content.contains(term)) {
				score += 1.0;
			}
			if (title.contains(term)) {
				score += 0.5;
			}
		}
		return score / Math.max(1, terms.size());
	}

	private boolean isStopWord(String term) {
		return Set.of("什么", "怎么", "如何", "为什么", "多少", "是否", "一下", "这个", "那个",
				"公司", "股份", "有限", "请", "帮我", "关于", "以及", "还有", "the", "and", "for", "what", "how").contains(term);
	}

	private boolean isCjk(char ch) {
		Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
		return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
				|| block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
				|| block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A;
	}

	private static class LinkedHashSetLike {
		private final List<String> values = new ArrayList<>();
		private final Set<String> seen = new HashSet<>();

		void add(String value) {
			if (seen.add(value)) {
				values.add(value);
			}
		}
	}
}
