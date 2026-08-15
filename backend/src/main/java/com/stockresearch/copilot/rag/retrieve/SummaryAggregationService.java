package com.stockresearch.copilot.rag.retrieve;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stockresearch.copilot.common.enums.DocType;
import com.stockresearch.copilot.common.enums.ProcessStatus;
import com.stockresearch.copilot.entity.Document;
import com.stockresearch.copilot.entity.DocumentChunk;
import com.stockresearch.copilot.mapper.DocumentChunkMapper;
import com.stockresearch.copilot.mapper.DocumentMapper;
import com.stockresearch.copilot.rag.intent.QuestionIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryAggregationService {

	private final DocumentMapper documentMapper;
	private final DocumentChunkMapper documentChunkMapper;

	public List<RetrievedChunk> aggregate(QuestionIntent intent, LocalDate startDate, LocalDate endDate,
									   List<String> docTypes, int topK) {
		Set<Long> documentIds = resolveDocumentIds(intent.getCompanyId(), startDate, endDate, docTypes);
		if (intent.getCompanyId() != null && documentIds.isEmpty()) {
			log.info("no READY summary documents for companyId={}", intent.getCompanyId());
			return List.of();
		}

		LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<>();
		if (intent.getCompanyId() != null) {
			wrapper.eq(DocumentChunk::getCompanyId, intent.getCompanyId());
		}
		if (!documentIds.isEmpty()) {
			wrapper.in(DocumentChunk::getDocumentId, documentIds);
		}
		wrapper.orderByAsc(DocumentChunk::getDocumentId)
				.orderByAsc(DocumentChunk::getChunkIndex);

		List<DocumentChunk> chunks = documentChunkMapper.selectList(wrapper);
		if (chunks.isEmpty()) {
			return List.of();
		}

		Map<Long, Document> documents = loadDocuments(chunks);
		Map<String, RetrievedChunk> byKey = new LinkedHashMap<>();
		for (DocumentChunk chunk : chunks) {
			String key = chunk.getDocumentId() + ":" + chunk.getTitlePath() + ":" + chunk.getSection();
			RetrievedChunk existing = byKey.get(key);
			double baseScore = scoreChunk(chunk, intent.getRawQuestion(), documents.get(chunk.getDocumentId()));
			if (existing == null) {
				byKey.put(key, RetrievedChunk.builder()
						.chunkId(chunk.getId())
						.documentId(chunk.getDocumentId())
						.companyId(chunk.getCompanyId())
						.chunk(chunk)
						.vectorScore(baseScore)
						.keywordScore(baseScore)
						.fusedScore(baseScore)
						.source("aggregate")
						.build());
			}
			else if (baseScore > existing.getFusedScore()) {
				existing.setChunkId(chunk.getId());
				existing.setChunk(chunk);
				existing.setFusedScore(baseScore);
				existing.setVectorScore(baseScore);
				existing.setKeywordScore(baseScore);
			}
		}

		List<RetrievedChunk> result = new ArrayList<>(byKey.values());
		result.sort(Comparator.comparingDouble(RetrievedChunk::getFusedScore).reversed());
		if (result.size() > topK) {
			return new ArrayList<>(result.subList(0, topK));
		}
		return result;
	}

	private Set<Long> resolveDocumentIds(Long companyId, LocalDate startDate, LocalDate endDate, List<String> docTypes) {
		LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<Document>()
				.eq(Document::getProcessStatus, ProcessStatus.READY.name())
				.eq(companyId != null, Document::getCompanyId, companyId)
				.ge(startDate != null, Document::getPublishDate, startDate)
				.le(endDate != null, Document::getPublishDate, endDate);
		if (docTypes != null && !docTypes.isEmpty()) {
			wrapper.in(Document::getDocType, docTypes.stream().map(DocType::from).map(DocType::name).toList());
		}
		return documentMapper.selectList(wrapper).stream()
				.map(Document::getId)
				.collect(Collectors.toSet());
	}

	private Map<Long, Document> loadDocuments(List<DocumentChunk> chunks) {
		List<Long> documentIds = chunks.stream().map(DocumentChunk::getDocumentId).distinct().toList();
		Map<Long, Document> map = new HashMap<>();
		if (documentIds.isEmpty()) {
			return map;
		}
		documentMapper.selectList(new LambdaQueryWrapper<Document>().in(Document::getId, documentIds))
				.forEach(doc -> map.put(doc.getId(), doc));
		return map;
	}

	private double scoreChunk(DocumentChunk chunk, String question, Document document) {
		double score = 0.2;
		if (StringUtils.hasText(question)) {
			String lowerQuestion = question.toLowerCase();
			String content = chunk.getContent() == null ? "" : chunk.getContent().toLowerCase();
			for (String token : lowerQuestion.split("[\\s，。！？；：、]+")) {
				String t = token.trim();
				if (t.length() >= 2 && content.contains(t)) {
					score += 0.2;
				}
			}
		}
		if (document != null) {
			String docType = document.getDocType() == null ? "" : document.getDocType();
			if (docType.contains("FINANCIAL")) {
				score += 0.2;
			}
			if (docType.contains("RESEARCH")) {
				score += 0.15;
			}
			if (StringUtils.hasText(document.getTitle())) {
				score += Math.min(0.15, document.getTitle().length() / 200.0);
			}
		}
		if (StringUtils.hasText(chunk.getTitlePath())) {
			score += Math.min(0.1, chunk.getTitlePath().length() / 300.0);
		}
		return score;
	}
}
