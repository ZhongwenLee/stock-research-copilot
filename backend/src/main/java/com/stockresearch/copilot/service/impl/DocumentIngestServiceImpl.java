package com.stockresearch.copilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stockresearch.copilot.common.enums.ProcessStatus;
import com.stockresearch.copilot.common.exception.BizException;
import com.stockresearch.copilot.common.exception.ErrorCode;
import com.stockresearch.copilot.common.metrics.RagMetrics;
import com.stockresearch.copilot.config.AppProperties;
import com.stockresearch.copilot.entity.Document;
import com.stockresearch.copilot.entity.DocumentChunk;
import com.stockresearch.copilot.mapper.DocumentChunkMapper;
import com.stockresearch.copilot.mapper.DocumentMapper;
import com.stockresearch.copilot.parser.ChunkDraft;
import com.stockresearch.copilot.parser.DocumentParserRegistry;
import com.stockresearch.copilot.parser.ParsedDocument;
import com.stockresearch.copilot.rag.embedding.EmbeddingClient;
import com.stockresearch.copilot.rag.vector.VectorRecord;
import com.stockresearch.copilot.rag.vector.VectorStore;
import com.stockresearch.copilot.service.ChunkingService;
import com.stockresearch.copilot.service.DocumentIngestService;
import com.stockresearch.copilot.service.FileStorageService;
import com.stockresearch.copilot.service.ReadyDocumentLookup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestServiceImpl implements DocumentIngestService {

	private final DocumentMapper documentMapper;
	private final DocumentChunkMapper documentChunkMapper;
	private final FileStorageService fileStorageService;
	private final DocumentParserRegistry documentParserRegistry;
	private final ChunkingService chunkingService;
	private final EmbeddingClient embeddingClient;
	private final VectorStore vectorStore;
	private final AppProperties appProperties;
	private final RagMetrics ragMetrics;
	private final ReadyDocumentLookup readyDocumentLookup;

	@Override
	@Async("documentIngestExecutor")
	public void ingestAsync(Long documentId) {
		ingest(documentId);
	}

	@Override
	public void ingest(Long documentId) {
		long started = System.currentTimeMillis();
		Document document = documentMapper.selectById(documentId);
		if (document == null) {
			log.warn("skip ingest, document missing id={}", documentId);
			return;
		}
		try {
			updateStatus(documentId, ProcessStatus.PARSING, null);
			Path filePath = fileStorageService.resolve(document.getStoragePath());
			ParsedDocument parsed = documentParserRegistry.parse(filePath, document.getFileExt());
			if (document.getTitle() == null || document.getTitle().isBlank()) {
				document.setTitle(parsed.getTitle());
				documentMapper.updateById(document);
			}

			updateStatus(documentId, ProcessStatus.CHUNKING, null);
			List<ChunkDraft> drafts = chunkingService.chunk(parsed);
			if (drafts.isEmpty()) {
				throw new BizException(ErrorCode.BAD_REQUEST, "no content extracted from document");
			}
			replaceChunks(document, drafts);

			updateStatus(documentId, ProcessStatus.EMBEDDING, null);
			embedAndStore(documentId);

			updateStatus(documentId, ProcessStatus.READY, null);
			readyDocumentLookup.evictAll();
			long latencyMs = System.currentTimeMillis() - started;
			ragMetrics.recordIngest(latencyMs);
			log.info("document ingest ready id={} chunks={} latencyMs={}", documentId, drafts.size(), latencyMs);
		}
		catch (Exception ex) {
			log.error("document ingest failed id={}", documentId, ex);
			String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
			if (message.length() > 1800) {
				message = message.substring(0, 1800);
			}
			updateStatus(documentId, ProcessStatus.FAILED, message);
			ragMetrics.markIngestFailure(documentId, message);
			ragMetrics.recordIngest(System.currentTimeMillis() - started);
		}
	}

	private void replaceChunks(Document document, List<ChunkDraft> drafts) {
		vectorStore.deleteByDocumentId(document.getId());
		documentChunkMapper.delete(new LambdaQueryWrapper<DocumentChunk>()
				.eq(DocumentChunk::getDocumentId, document.getId()));

		for (ChunkDraft draft : drafts) {
			DocumentChunk chunk = new DocumentChunk();
			chunk.setDocumentId(document.getId());
			chunk.setCompanyId(document.getCompanyId());
			chunk.setChunkIndex(draft.getChunkIndex());
			chunk.setTitlePath(draft.getTitlePath());
			chunk.setSection(draft.getSection());
			chunk.setContent(draft.getContent());
			chunk.setPageNo(draft.getPageNo());
			chunk.setTokenCount(draft.getTokenCount());
			documentChunkMapper.insert(chunk);
		}
	}

	private void embedAndStore(Long documentId) {
		Document document = documentMapper.selectById(documentId);
		List<DocumentChunk> chunks = documentChunkMapper.selectList(new LambdaQueryWrapper<DocumentChunk>()
				.eq(DocumentChunk::getDocumentId, documentId)
				.orderByAsc(DocumentChunk::getChunkIndex));
		int batchSize = Math.max(1, appProperties.getIngest().getEmbeddingBatchSize());

		for (int i = 0; i < chunks.size(); i += batchSize) {
			long embedStarted = System.currentTimeMillis();
			List<DocumentChunk> batch = chunks.subList(i, Math.min(i + batchSize, chunks.size()));
			List<String> texts = batch.stream().map(DocumentChunk::getContent).toList();
			List<float[]> vectors = embeddingClient.embed(texts);
			ragMetrics.recordEmbed(System.currentTimeMillis() - embedStarted);
			List<VectorRecord> records = new ArrayList<>(batch.size());
			for (int j = 0; j < batch.size(); j++) {
				DocumentChunk chunk = batch.get(j);
				String vectorId = "chk_" + chunk.getId() + "_" + UUID.randomUUID().toString().replace("-", "");
				chunk.setVectorId(vectorId);
				documentChunkMapper.updateById(chunk);

				Map<String, Object> metadata = new HashMap<>();
				metadata.put("docType", document.getDocType());
				metadata.put("titlePath", chunk.getTitlePath());
				metadata.put("pageNo", chunk.getPageNo());
				records.add(VectorRecord.builder()
						.vectorId(vectorId)
						.chunkId(chunk.getId())
						.documentId(document.getId())
						.companyId(document.getCompanyId())
						.embedding(vectors.get(j))
						.metadata(metadata)
						.build());
			}
			vectorStore.upsert(records);
		}
	}

	private void updateStatus(Long documentId, ProcessStatus status, String errorMessage) {
		Document patch = new Document();
		patch.setId(documentId);
		patch.setProcessStatus(status.name());
		patch.setErrorMessage(errorMessage);
		documentMapper.updateById(patch);
	}
}
