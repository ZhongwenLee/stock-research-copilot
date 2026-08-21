package com.stockresearch.copilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stockresearch.copilot.common.enums.CitationRefType;
import com.stockresearch.copilot.common.enums.SummaryMode;
import com.stockresearch.copilot.common.exception.BizException;
import com.stockresearch.copilot.common.exception.ErrorCode;
import com.stockresearch.copilot.common.metrics.RagMetrics;
import com.stockresearch.copilot.config.AppProperties;
import com.stockresearch.copilot.dto.SummaryGenerateRequest;
import com.stockresearch.copilot.dto.SummaryQueryRequest;
import com.stockresearch.copilot.entity.Citation;
import com.stockresearch.copilot.entity.Company;
import com.stockresearch.copilot.entity.Document;
import com.stockresearch.copilot.entity.Summary;
import com.stockresearch.copilot.mapper.CitationMapper;
import com.stockresearch.copilot.mapper.CompanyMapper;
import com.stockresearch.copilot.mapper.DocumentMapper;
import com.stockresearch.copilot.mapper.SummaryMapper;
import com.stockresearch.copilot.rag.context.SummaryContext;
import com.stockresearch.copilot.rag.context.SummaryContextBuilder;
import com.stockresearch.copilot.rag.context.SummaryTemplate;
import com.stockresearch.copilot.rag.intent.QuestionIntent;
import com.stockresearch.copilot.rag.llm.ChatClient;
import com.stockresearch.copilot.rag.rerank.HeuristicRerankService;
import com.stockresearch.copilot.rag.retrieve.RetrievedChunk;
import com.stockresearch.copilot.rag.retrieve.SummaryAggregationService;
import com.stockresearch.copilot.service.SummaryService;
import com.stockresearch.copilot.service.support.DocumentConverters;
import com.stockresearch.copilot.vo.CitationVO;
import com.stockresearch.copilot.vo.DocumentChunkVO;
import com.stockresearch.copilot.vo.SummaryAnswerVO;
import com.stockresearch.copilot.vo.SummaryHistoryVO;
import com.stockresearch.copilot.vo.SummarySectionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryServiceImpl implements SummaryService {

	private static final Pattern CITE_PATTERN = Pattern.compile("\\[(\\d+)]");

	private final CompanyMapper companyMapper;
	private final SummaryAggregationService summaryAggregationService;
	private final HeuristicRerankService heuristicRerankService;
	private final SummaryContextBuilder summaryContextBuilder;
	private final ChatClient chatClient;
	private final SummaryMapper summaryMapper;
	private final CitationMapper citationMapper;
	private final DocumentMapper documentMapper;
	private final AppProperties appProperties;
	private final RagMetrics ragMetrics;

	@Override
	@Transactional
	public SummaryAnswerVO generate(SummaryGenerateRequest request) {
		long started = System.currentTimeMillis();
		try {
			Company company = companyMapper.selectById(request.getCompanyId());
			if (company == null) {
				throw new IllegalArgumentException("company not found: " + request.getCompanyId());
			}

			SummaryMode mode = request.getMode() == null ? SummaryMode.FAST : request.getMode();
			QuestionIntent intent = QuestionIntent.builder()
					.intentType(com.stockresearch.copilot.common.enums.IntentType.SUMMARY)
					.companyId(company.getId())
					.companyName(company.getName())
					.stockCode(StringUtils.hasText(request.getStockCode()) ? request.getStockCode() : company.getStockCode())
					.preferredDocTypes(request.getDocTypes() == null ? List.of() : request.getDocTypes().stream()
							.map(com.stockresearch.copilot.common.enums.DocType::from)
							.toList())
					.rawQuestion("请生成公司研究摘要")
					.build();

			int topK = request.getTopK() == null
					? SummaryTemplateDefaults.topK(mode)
					: Math.max(1, Math.min(request.getTopK(), 20));

			List<RetrievedChunk> aggregated = summaryAggregationService.aggregate(
					intent,
					request.getStartDate(),
					request.getEndDate(),
					request.getDocTypes(),
					topK * 3);
			List<RetrievedChunk> ranked = heuristicRerankService.rerank(intent.getRawQuestion(), aggregated, topK);
			SummaryContext summaryContext = summaryContextBuilder.build(intent, ranked, mode);

			long generateStarted = System.currentTimeMillis();
			String answer = chatClient.chat(summaryContext.getSystemPrompt(), summaryContext.getUserPrompt());
			ragMetrics.recordGenerate(System.currentTimeMillis() - generateStarted);

			List<SummarySectionVO> sections = buildSections(answer, summaryContext.getTemplate().getSections());
			List<CitationVO> citations = buildCitations(answer, summaryContext.getUsedChunks());
			List<DocumentChunkVO> chunks = summaryContext.getUsedChunks().stream()
					.map(RetrievedChunk::getChunk)
					.filter(chunk -> chunk != null)
					.map(DocumentConverters::toChunkVO)
					.toList();
			boolean insufficient = summaryContext.getUsedChunks().isEmpty() || containsInsufficientSignal(answer);
			String overview = sections.isEmpty() ? answer : sections.get(0).getContent();
			String title = company.getName() + " " + summaryContext.getTemplate().getTitle();
			long latency = System.currentTimeMillis() - started;

			Summary summary = persist(company, request, mode, title, overview, sections, latency);
			persistCitations(summary.getId(), citations);
			ragMetrics.markQaSuccess(insufficient);

			log.info("summary done summaryId={} companyId={} mode={} recall={} used={} latencyMs={} insufficient={}",
					summary.getId(), company.getId(), mode,
					aggregated.size(), summaryContext.getUsedChunks().size(), latency, insufficient);

			return SummaryAnswerVO.builder()
					.summaryId(summary.getId())
					.companyId(company.getId())
					.companyName(company.getName())
					.stockCode(company.getStockCode())
					.mode(mode.name())
					.title(title)
					.overview(overview)
					.sections(sections)
					.citations(citations)
					.chunks(chunks)
					.docTypes(request.getDocTypes() == null ? List.of() : new ArrayList<>(request.getDocTypes()))
					.startDate(request.getStartDate())
					.endDate(request.getEndDate())
					.latencyMs(latency)
					.insufficientEvidence(insufficient)
					.build();
		}
		catch (RuntimeException ex) {
			ragMetrics.markQaFailure();
			String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(Locale.ROOT);
			if (message.contains("timeout") || message.contains("connection") || message.contains("openai")) {
				ragMetrics.markAiUnavailable("summary", ex.getMessage());
				throw new BizException(ErrorCode.AI_UNAVAILABLE, "摘要生成服务暂不可用，请稍后重试");
			}
			throw ex;
		}
	}

	@Override
	public com.stockresearch.copilot.common.result.PageResult<SummaryHistoryVO> history(SummaryQueryRequest request) {
		Page<Summary> page = new Page<>(request.getPageNum(), request.getPageSize());
		LambdaQueryWrapper<Summary> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(request.getCompanyId() != null, Summary::getCompanyId, request.getCompanyId())
				.eq(StringUtils.hasText(request.getMode()), Summary::getMode, request.getMode())
				.ge(request.getStartDate() != null, Summary::getStartDate, request.getStartDate())
				.le(request.getEndDate() != null, Summary::getEndDate, request.getEndDate())
				.like(StringUtils.hasText(request.getKeyword()), Summary::getTitle, request.getKeyword())
				.orderByDesc(Summary::getId);
		Page<Summary> result = summaryMapper.selectPage(page, wrapper);
		List<SummaryHistoryVO> records = result.getRecords().stream()
				.map(this::toHistoryVO)
				.toList();
		return com.stockresearch.copilot.common.result.PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
	}

	@Override
	public SummaryHistoryVO getHistoryById(Long summaryId) {
		Summary summary = summaryMapper.selectById(summaryId);
		if (summary == null) {
			throw new IllegalArgumentException("summary not found: " + summaryId);
		}
		return toHistoryVO(summary);
	}

	private SummaryHistoryVO toHistoryVO(Summary summary) {
		Company company = summary.getCompanyId() == null ? null : companyMapper.selectById(summary.getCompanyId());
		long citationCount = citationMapper.selectCount(new LambdaQueryWrapper<Citation>()
				.eq(Citation::getRefType, CitationRefType.SUMMARY.name())
				.eq(Citation::getRefId, summary.getId()));
		return SummaryHistoryVO.builder()
				.summaryId(summary.getId())
				.companyId(summary.getCompanyId())
				.companyName(company == null ? null : company.getName())
				.stockCode(summary.getStockCode())
				.mode(summary.getMode())
				.title(summary.getTitle())
				.overview(summary.getOverview())
				.docTypes(parseJsonArray(summary.getDocTypesJson()))
				.startDate(summary.getStartDate())
				.endDate(summary.getEndDate())
				.latencyMs(summary.getLatencyMs())
				.insufficientEvidence(containsInsufficientSignal(summary.getOverview()))
				.citationCount((int) citationCount)
				.createdAt(summary.getCreatedAt())
				.build();
	}

	private Summary persist(Company company, SummaryGenerateRequest request, SummaryMode mode,
							String title, String overview, List<SummarySectionVO> sections, long latency) {
		Summary summary = new Summary();
		summary.setCompanyId(company.getId());
		summary.setStockCode(StringUtils.hasText(request.getStockCode()) ? request.getStockCode() : company.getStockCode());
		summary.setMode(mode.name());
		summary.setTitle(title);
		summary.setOverview(overview);
		summary.setSectionsJson(writeJson(sections));
		summary.setDocTypesJson(writeJson(request.getDocTypes()));
		summary.setStartDate(request.getStartDate());
		summary.setEndDate(request.getEndDate());
		summary.setLatencyMs(latency);
		summary.setStatus("DONE");
		summaryMapper.insert(summary);
		return summary;
	}

	private void persistCitations(Long summaryId, List<CitationVO> citations) {
		int rank = 1;
		for (CitationVO citationVO : citations) {
			Citation citation = new Citation();
			citation.setRefType(CitationRefType.SUMMARY.name());
			citation.setRefId(summaryId);
			citation.setChunkId(citationVO.getChunkId());
			citation.setQuoteText(citationVO.getQuoteText());
			citation.setRankNo(rank++);
			citation.setScore(citationVO.getScore());
			citationMapper.insert(citation);
		}
	}

	private List<SummarySectionVO> buildSections(String answer, List<String> sectionTitles) {
		if (!StringUtils.hasText(answer)) {
			return List.of();
		}
		Map<String, StringBuilder> sectionMap = new HashMap<>();
		String current = null;
		for (String rawLine : answer.split("\\R")) {
			String line = rawLine.trim();
			if (!StringUtils.hasText(line)) {
				continue;
			}
			String matched = matchSectionTitle(line, sectionTitles);
			if (matched != null) {
				current = matched;
				sectionMap.putIfAbsent(current, new StringBuilder());
				String remainder = stripHeading(line, matched);
				if (StringUtils.hasText(remainder)) {
					sectionMap.get(current).append(remainder).append('\n');
				}
				continue;
			}
			if (current == null) {
				current = sectionTitles.isEmpty() ? "摘要" : sectionTitles.get(0);
				sectionMap.putIfAbsent(current, new StringBuilder());
			}
			sectionMap.get(current).append(line).append('\n');
		}

		if (sectionMap.isEmpty()) {
			return List.of(SummarySectionVO.builder()
					.title(sectionTitles.isEmpty() ? "摘要" : sectionTitles.get(0))
					.content(answer)
					.charCount(answer.length())
					.citationCount(countCitations(answer))
					.build());
		}

		List<SummarySectionVO> sections = new ArrayList<>();
		for (String title : sectionTitles) {
			String content = sectionMap.containsKey(title) ? sectionMap.get(title).toString().trim() : "";
			if (!StringUtils.hasText(content)) {
				continue;
			}
			sections.add(SummarySectionVO.builder()
					.title(title)
					.content(content)
					.charCount(content.length())
					.citationCount(countCitations(content))
					.build());
		}
		if (sections.isEmpty()) {
			sections.add(SummarySectionVO.builder()
					.title(sectionTitles.isEmpty() ? "摘要" : sectionTitles.get(0))
					.content(answer)
					.charCount(answer.length())
					.citationCount(countCitations(answer))
					.build());
		}
		return sections;
	}

	private String matchSectionTitle(String line, List<String> sectionTitles) {
		String normalized = normalizeHeading(line);
		for (String title : sectionTitles) {
			if (normalized.equals(normalizeHeading(title))) {
				return title;
			}
		}
		return null;
	}

	private String stripHeading(String line, String title) {
		String normalized = normalizeHeading(line);
		String normalizedTitle = normalizeHeading(title);
		if (normalized.startsWith(normalizedTitle)) {
			String remainder = line.substring(Math.min(line.length(), title.length())).trim();
			if (remainder.startsWith(":") || remainder.startsWith("：")) {
				remainder = remainder.substring(1).trim();
			}
			return remainder;
		}
		return line;
	}

	private String normalizeHeading(String value) {
		return value.replaceAll("^[#\\s]+", "")
				.replaceFirst("^[一二三四五六七八九十0-9]+[、.．]\\s*", "")
				.replaceAll("[：:]$", "")
				.trim();
	}

	private List<CitationVO> buildCitations(String answer, List<RetrievedChunk> usedChunks) {
		if (usedChunks == null || usedChunks.isEmpty()) {
			return List.of();
		}
		Map<Long, Document> documentMap = loadDocuments(usedChunks);
		List<Integer> citedIndexes = parseCitedIndexes(answer, usedChunks.size());
		List<RetrievedChunk> selected = citedIndexes.isEmpty() ? usedChunks : new ArrayList<>();
		if (!citedIndexes.isEmpty()) {
			for (Integer index : citedIndexes) {
				selected.add(usedChunks.get(index - 1));
			}
		}

		int quoteMax = Math.max(80, appProperties.getQa().getQuoteMaxChars());
		List<CitationVO> citations = new ArrayList<>();
		int rank = 1;
		for (RetrievedChunk item : selected) {
			if (item.getChunk() == null) {
				continue;
			}
			Document document = documentMap.get(item.getChunk().getDocumentId());
			citations.add(CitationVO.builder()
					.chunkId(item.getChunk().getId())
					.documentId(item.getChunk().getDocumentId())
					.documentTitle(document == null ? null : document.getTitle())
					.docType(document == null ? null : document.getDocType())
					.quoteText(excerpt(item.getChunk().getContent(), quoteMax))
					.rankNo(rank++)
					.score(item.getFusedScore())
					.titlePath(item.getChunk().getTitlePath())
					.pageNo(item.getChunk().getPageNo())
					.section(item.getChunk().getSection())
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

	private int countCitations(String text) {
		if (!StringUtils.hasText(text)) {
			return 0;
		}
		Matcher matcher = CITE_PATTERN.matcher(text);
		int count = 0;
		while (matcher.find()) {
			count++;
		}
		return count;
	}

	private boolean containsInsufficientSignal(String answer) {
		if (!StringUtils.hasText(answer)) {
			return true;
		}
		String lower = answer.toLowerCase(Locale.ROOT);
		return lower.contains("依据不足") || lower.contains("不足以") || lower.contains("无法判断")
				|| lower.contains("insufficient");
	}

	private List<String> parseJsonArray(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		String trimmed = json.trim();
		if (trimmed.length() < 2) {
			return List.of(trimmed);
		}
		String body = trimmed.substring(1, trimmed.length() - 1).trim();
		if (!StringUtils.hasText(body)) {
			return List.of();
		}
		String[] parts = body.split(",");
		List<String> result = new ArrayList<>();
		for (String part : parts) {
			String item = part.trim();
			if (item.startsWith("\"") && item.endsWith("\"") && item.length() >= 2) {
				item = item.substring(1, item.length() - 1);
			}
			if (StringUtils.hasText(item)) {
				result.add(item);
			}
		}
		return result;
	}

	private String writeJson(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Collection<?> collection) {
			return collection.stream().map(this::scalarJson).collect(Collectors.joining(",", "[", "]"));
		}
		return scalarJson(value);
	}

	private String scalarJson(Object value) {
		if (value == null) {
			return "null";
		}
		if (value instanceof Number || value instanceof Boolean) {
			return String.valueOf(value);
		}
		String text = String.valueOf(value)
				.replace("\\", "\\\\")
				.replace("\"", "\\\"");
		return "\"" + text + "\"";
	}

	private static final class SummaryTemplateDefaults {
		private static int topK(SummaryMode mode) {
			return mode == SummaryMode.DEEP ? 12 : 6;
		}
	}
}
