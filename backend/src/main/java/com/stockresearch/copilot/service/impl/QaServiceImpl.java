package com.stockresearch.copilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stockresearch.copilot.common.enums.CitationRefType;
import com.stockresearch.copilot.common.enums.DocType;
import com.stockresearch.copilot.config.AppProperties;
import com.stockresearch.copilot.dto.QaAskRequest;
import com.stockresearch.copilot.entity.Citation;
import com.stockresearch.copilot.entity.Document;
import com.stockresearch.copilot.entity.DocumentChunk;
import com.stockresearch.copilot.entity.Question;
import com.stockresearch.copilot.mapper.CitationMapper;
import com.stockresearch.copilot.mapper.DocumentMapper;
import com.stockresearch.copilot.mapper.QuestionMapper;
import com.stockresearch.copilot.rag.context.ContextBuilder;
import com.stockresearch.copilot.rag.context.PromptContext;
import com.stockresearch.copilot.rag.intent.IntentRecognizer;
import com.stockresearch.copilot.rag.intent.QuestionIntent;
import com.stockresearch.copilot.rag.llm.ChatClient;
import com.stockresearch.copilot.rag.rerank.HeuristicRerankService;
import com.stockresearch.copilot.rag.retrieve.HybridRetrievalService;
import com.stockresearch.copilot.rag.retrieve.RetrievedChunk;
import com.stockresearch.copilot.service.QaService;
import com.stockresearch.copilot.service.support.DocumentConverters;
import com.stockresearch.copilot.vo.CitationVO;
import com.stockresearch.copilot.vo.DocumentChunkVO;
import com.stockresearch.copilot.vo.QaAnswerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class QaServiceImpl implements QaService {

	private static final Pattern CITE_PATTERN = Pattern.compile("\\[(\\d+)]");

	private final IntentRecognizer intentRecognizer;
	private final HybridRetrievalService hybridRetrievalService;
	private final HeuristicRerankService heuristicRerankService;
	private final ContextBuilder contextBuilder;
	private final ChatClient chatClient;
	private final QuestionMapper questionMapper;
	private final CitationMapper citationMapper;
	private final DocumentMapper documentMapper;
	private final AppProperties appProperties;

	@Override
	@Transactional
	public QaAnswerVO ask(QaAskRequest request) {
		long started = System.currentTimeMillis();
		QuestionIntent intent = intentRecognizer.recognize(
				request.getQuestion(),
				request.getCompanyId(),
				request.getStockCode(),
				request.getDocTypes());

		int recallTopK = appProperties.getQa().getRecallTopK();
		int rerankTopK = request.getTopK() == null
				? appProperties.getQa().getRerankTopK()
				: Math.max(1, Math.min(request.getTopK(), 20));

		List<RetrievedChunk> recalled = hybridRetrievalService.retrieve(intent, recallTopK);
		List<RetrievedChunk> ranked = heuristicRerankService.rerank(intent.getRawQuestion(), recalled, rerankTopK);
		PromptContext promptContext = contextBuilder.build(intent, ranked);
		String answer = chatClient.chat(promptContext.getSystemPrompt(), promptContext.getUserPrompt());

		boolean insufficient = promptContext.getUsedChunks().isEmpty()
				|| containsInsufficientSignal(answer);

		List<CitationVO> citations = buildCitations(answer, promptContext.getUsedChunks());
		List<DocumentChunkVO> chunks = promptContext.getUsedChunks().stream()
				.map(RetrievedChunk::getChunk)
				.filter(chunk -> chunk != null)
				.map(DocumentConverters::toChunkVO)
				.toList();

		long latency = System.currentTimeMillis() - started;
		Question question = persist(intent, answer, latency, citations);

		log.info("qa done questionId={} companyId={} intent={} recalled={} used={} latencyMs={}",
				question.getId(), intent.getCompanyId(), intent.getIntentType(),
				recalled.size(), promptContext.getUsedChunks().size(), latency);

		return QaAnswerVO.builder()
				.questionId(question.getId())
				.question(intent.getRawQuestion())
				.answer(answer)
				.intentType(intent.getIntentType().name())
				.companyId(intent.getCompanyId())
				.companyName(intent.getCompanyName())
				.stockCode(intent.getStockCode())
				.preferredDocTypes(intent.getPreferredDocTypes() == null
						? List.of()
						: intent.getPreferredDocTypes().stream().map(DocType::name).toList())
				.insufficientEvidence(insufficient)
				.citations(citations)
				.chunks(chunks)
				.latencyMs(latency)
				.build();
	}

	private Question persist(QuestionIntent intent, String answer, long latency, List<CitationVO> citations) {
		Question question = new Question();
		question.setCompanyId(intent.getCompanyId());
		question.setQuestionText(intent.getRawQuestion());
		question.setAnswerText(answer);
		question.setIntentType(intent.getIntentType().name());
		question.setLatencyMs(latency);
		questionMapper.insert(question);

		int rank = 1;
		for (CitationVO citationVO : citations) {
			Citation citation = new Citation();
			citation.setRefType(CitationRefType.QUESTION.name());
			citation.setRefId(question.getId());
			citation.setChunkId(citationVO.getChunkId());
			citation.setQuoteText(citationVO.getQuoteText());
			citation.setRankNo(rank++);
			citation.setScore(citationVO.getScore());
			citationMapper.insert(citation);
		}
		return question;
	}

	private List<CitationVO> buildCitations(String answer, List<RetrievedChunk> usedChunks) {
		if (usedChunks == null || usedChunks.isEmpty()) {
			return List.of();
		}
		Map<Long, Document> documentMap = loadDocuments(usedChunks);
		List<Integer> citedIndexes = parseCitedIndexes(answer, usedChunks.size());
		List<RetrievedChunk> selected;
		if (citedIndexes.isEmpty()) {
			selected = usedChunks;
		}
		else {
			selected = new ArrayList<>();
			for (Integer index : citedIndexes) {
				selected.add(usedChunks.get(index - 1));
			}
		}

		int quoteMax = Math.max(80, appProperties.getQa().getQuoteMaxChars());
		List<CitationVO> citations = new ArrayList<>();
		int rank = 1;
		for (RetrievedChunk item : selected) {
			DocumentChunk chunk = item.getChunk();
			if (chunk == null) {
				continue;
			}
			Document document = documentMap.get(chunk.getDocumentId());
			citations.add(CitationVO.builder()
					.chunkId(chunk.getId())
					.documentId(chunk.getDocumentId())
					.documentTitle(document == null ? null : document.getTitle())
					.docType(document == null ? null : document.getDocType())
					.quoteText(excerpt(chunk.getContent(), quoteMax))
					.rankNo(rank++)
					.score(item.getFusedScore())
					.titlePath(chunk.getTitlePath())
					.pageNo(chunk.getPageNo())
					.section(chunk.getSection())
					.build());
		}
		return citations;
	}

	private Map<Long, Document> loadDocuments(List<RetrievedChunk> usedChunks) {
		List<Long> documentIds = usedChunks.stream()
				.map(RetrievedChunk::getDocumentId)
				.filter(id -> id != null)
				.distinct()
				.toList();
		if (documentIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, Document> map = new HashMap<>();
		documentMapper.selectList(new LambdaQueryWrapper<Document>().in(Document::getId, documentIds))
				.forEach(doc -> map.put(doc.getId(), doc));
		return map;
	}

	private List<Integer> parseCitedIndexes(String answer, int maxIndex) {
		if (!StringUtils.hasText(answer)) {
			return List.of();
		}
		Matcher matcher = CITE_PATTERN.matcher(answer);
		List<Integer> indexes = new ArrayList<>();
		while (matcher.find()) {
			int value = Integer.parseInt(matcher.group(1));
			if (value >= 1 && value <= maxIndex && !indexes.contains(value)) {
				indexes.add(value);
			}
		}
		return indexes;
	}

	private String excerpt(String content, int maxChars) {
		if (!StringUtils.hasText(content)) {
			return "";
		}
		String normalized = content.replaceAll("\\s+", " ").trim();
		if (normalized.length() <= maxChars) {
			return normalized;
		}
		return normalized.substring(0, maxChars) + "…";
	}

	private boolean containsInsufficientSignal(String answer) {
		if (!StringUtils.hasText(answer)) {
			return true;
		}
		String lower = answer.toLowerCase(Locale.ROOT);
		return lower.contains("依据不足") || lower.contains("不足以") || lower.contains("无法判断")
				|| lower.contains("insufficient");
	}
}
