package com.stockresearch.copilot.service.support;

import com.stockresearch.copilot.entity.Document;
import com.stockresearch.copilot.entity.DocumentChunk;
import com.stockresearch.copilot.vo.DocumentChunkVO;
import com.stockresearch.copilot.vo.DocumentVO;

public final class DocumentConverters {

	private DocumentConverters() {
	}

	public static DocumentVO toDocumentVO(Document document) {
		return DocumentVO.builder()
				.id(document.getId())
				.companyId(document.getCompanyId())
				.title(document.getTitle())
				.docType(document.getDocType())
				.fileName(document.getFileName())
				.fileExt(document.getFileExt())
				.fileSize(document.getFileSize())
				.publishDate(document.getPublishDate())
				.processStatus(document.getProcessStatus())
				.errorMessage(document.getErrorMessage())
				.createdAt(document.getCreatedAt())
				.updatedAt(document.getUpdatedAt())
				.build();
	}

	public static DocumentChunkVO toChunkVO(DocumentChunk chunk) {
		return DocumentChunkVO.builder()
				.id(chunk.getId())
				.documentId(chunk.getDocumentId())
				.companyId(chunk.getCompanyId())
				.chunkIndex(chunk.getChunkIndex())
				.titlePath(chunk.getTitlePath())
				.content(chunk.getContent())
				.pageNo(chunk.getPageNo())
				.section(chunk.getSection())
				.tokenCount(chunk.getTokenCount())
				.vectorId(chunk.getVectorId())
				.createdAt(chunk.getCreatedAt())
				.build();
	}
}
