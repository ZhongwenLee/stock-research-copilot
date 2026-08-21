package com.stockresearch.copilot.service.impl;

import com.stockresearch.copilot.common.enums.ProcessStatus;
import com.stockresearch.copilot.common.metrics.RagMetrics;
import com.stockresearch.copilot.config.AppProperties;
import com.stockresearch.copilot.entity.Document;
import com.stockresearch.copilot.entity.DocumentChunk;
import com.stockresearch.copilot.mapper.DocumentChunkMapper;
import com.stockresearch.copilot.mapper.DocumentMapper;
import com.stockresearch.copilot.parser.ChunkDraft;
import com.stockresearch.copilot.parser.DocumentParserRegistry;
import com.stockresearch.copilot.parser.ParsedDocument;
import com.stockresearch.copilot.parser.ParsedSection;
import com.stockresearch.copilot.rag.embedding.EmbeddingClient;
import com.stockresearch.copilot.rag.vector.VectorStore;
import com.stockresearch.copilot.service.ChunkingService;
import com.stockresearch.copilot.service.FileStorageService;
import com.stockresearch.copilot.service.ReadyDocumentLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentIngestServiceImplTest {

	@Mock private DocumentMapper documentMapper;
	@Mock private DocumentChunkMapper documentChunkMapper;
	@Mock private FileStorageService fileStorageService;
	@Mock private DocumentParserRegistry documentParserRegistry;
	@Mock private ChunkingService chunkingService;
	@Mock private EmbeddingClient embeddingClient;
	@Mock private VectorStore vectorStore;
	@Mock private RagMetrics ragMetrics;
	@Mock private ReadyDocumentLookup readyDocumentLookup;

	private DocumentIngestServiceImpl service;

	@BeforeEach
	void setUp() {
		AppProperties props = new AppProperties();
		props.getIngest().setEmbeddingBatchSize(2);
		service = new DocumentIngestServiceImpl(
				documentMapper, documentChunkMapper, fileStorageService, documentParserRegistry,
				chunkingService, embeddingClient, vectorStore, props, ragMetrics, readyDocumentLookup);
	}

	@Test
	void ingestMarksReadyAfterEmbed() {
		Document document = new Document();
		document.setId(5L);
		document.setCompanyId(1L);
		document.setStoragePath("a/b.txt");
		document.setFileExt("txt");
		document.setDocType("ANNOUNCEMENT");
		document.setTitle("公告");
		when(documentMapper.selectById(5L)).thenReturn(document);
		when(fileStorageService.resolve(anyString())).thenReturn(Path.of("a/b.txt"));
		when(documentParserRegistry.parse(any(), anyString())).thenReturn(ParsedDocument.builder()
				.title("公告")
				.sections(List.of(ParsedSection.builder().titlePath("正文").section("正文").content("内容").build()))
				.build());
		when(chunkingService.chunk(any())).thenReturn(List.of(
				ChunkDraft.builder().chunkIndex(0).content("内容一").titlePath("正文").tokenCount(2).build(),
				ChunkDraft.builder().chunkIndex(1).content("内容二").titlePath("正文").tokenCount(2).build()
		));
		doAnswer(invocation -> {
			DocumentChunk chunk = invocation.getArgument(0);
			chunk.setId(chunk.getChunkIndex() == 0 ? 101L : 102L);
			return 1;
		}).when(documentChunkMapper).insert(any(DocumentChunk.class));

		DocumentChunk c1 = new DocumentChunk();
		c1.setId(101L);
		c1.setDocumentId(5L);
		c1.setCompanyId(1L);
		c1.setChunkIndex(0);
		c1.setContent("内容一");
		DocumentChunk c2 = new DocumentChunk();
		c2.setId(102L);
		c2.setDocumentId(5L);
		c2.setCompanyId(1L);
		c2.setChunkIndex(1);
		c2.setContent("内容二");
		when(documentChunkMapper.selectList(any())).thenReturn(List.of(c1, c2));
		when(embeddingClient.embed(any())).thenReturn(List.of(new float[] {0.1f}, new float[] {0.2f}));

		service.ingest(5L);

		ArgumentCaptor<Document> statusCaptor = ArgumentCaptor.forClass(Document.class);
		verify(documentMapper, atLeastOnce()).updateById(statusCaptor.capture());
		assertEquals(ProcessStatus.READY.name(),
				statusCaptor.getAllValues().get(statusCaptor.getAllValues().size() - 1).getProcessStatus());
		verify(vectorStore).upsert(any());
		verify(readyDocumentLookup).evictAll();
		verify(ragMetrics).recordIngest(anyLong());
	}

	@Test
	void ingestMarksFailedOnEmptyChunks() {
		Document document = new Document();
		document.setId(6L);
		document.setStoragePath("x.txt");
		document.setFileExt("txt");
		when(documentMapper.selectById(6L)).thenReturn(document);
		when(fileStorageService.resolve(anyString())).thenReturn(Path.of("x.txt"));
		when(documentParserRegistry.parse(any(), anyString())).thenReturn(ParsedDocument.builder()
				.title("empty")
				.sections(List.of())
				.build());
		when(chunkingService.chunk(any())).thenReturn(List.of());

		service.ingest(6L);

		ArgumentCaptor<Document> statusCaptor = ArgumentCaptor.forClass(Document.class);
		verify(documentMapper, atLeastOnce()).updateById(statusCaptor.capture());
		assertEquals(ProcessStatus.FAILED.name(),
				statusCaptor.getAllValues().get(statusCaptor.getAllValues().size() - 1).getProcessStatus());
		verify(ragMetrics).markIngestFailure(anyLong(), anyString());
	}
}
