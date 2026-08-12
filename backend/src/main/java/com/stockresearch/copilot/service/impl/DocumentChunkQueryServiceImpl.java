package com.stockresearch.copilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stockresearch.copilot.common.exception.BizException;
import com.stockresearch.copilot.common.exception.ErrorCode;
import com.stockresearch.copilot.common.result.PageResult;
import com.stockresearch.copilot.dto.ChunkQueryRequest;
import com.stockresearch.copilot.entity.Document;
import com.stockresearch.copilot.entity.DocumentChunk;
import com.stockresearch.copilot.mapper.DocumentChunkMapper;
import com.stockresearch.copilot.mapper.DocumentMapper;
import com.stockresearch.copilot.service.DocumentChunkQueryService;
import com.stockresearch.copilot.service.support.DocumentConverters;
import com.stockresearch.copilot.vo.DocumentChunkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentChunkQueryServiceImpl implements DocumentChunkQueryService {

	private final DocumentChunkMapper documentChunkMapper;
	private final DocumentMapper documentMapper;

	@Override
	public DocumentChunkVO getById(Long id) {
		DocumentChunk chunk = documentChunkMapper.selectById(id);
		if (chunk == null) {
			throw new BizException(ErrorCode.NOT_FOUND, "chunk not found: " + id);
		}
		return DocumentConverters.toChunkVO(chunk);
	}

	@Override
	public PageResult<DocumentChunkVO> page(ChunkQueryRequest request) {
		Set<Long> documentIds = resolveDocumentIds(request);
		if (documentIds != null && documentIds.isEmpty()) {
			return PageResult.empty(request.getPageNum(), request.getPageSize());
		}

		Page<DocumentChunk> page = new Page<>(request.getPageNum(), request.getPageSize());
		LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(request.getDocumentId() != null, DocumentChunk::getDocumentId, request.getDocumentId())
				.eq(request.getCompanyId() != null, DocumentChunk::getCompanyId, request.getCompanyId())
				.in(documentIds != null, DocumentChunk::getDocumentId, documentIds)
				.orderByAsc(DocumentChunk::getDocumentId)
				.orderByAsc(DocumentChunk::getChunkIndex);

		Page<DocumentChunk> result = documentChunkMapper.selectPage(page, wrapper);
		List<DocumentChunkVO> records = result.getRecords().stream().map(DocumentConverters::toChunkVO).toList();
		return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
	}

	/**
	 * When start/end date provided, filter by document.publish_date.
	 * Returns null when no date filter is needed.
	 */
	private Set<Long> resolveDocumentIds(ChunkQueryRequest request) {
		if (request.getStartDate() == null && request.getEndDate() == null) {
			return null;
		}
		LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(request.getCompanyId() != null, Document::getCompanyId, request.getCompanyId())
				.eq(request.getDocumentId() != null, Document::getId, request.getDocumentId())
				.ge(request.getStartDate() != null, Document::getPublishDate, request.getStartDate())
				.le(request.getEndDate() != null, Document::getPublishDate, request.getEndDate())
				.select(Document::getId);
		List<Document> documents = documentMapper.selectList(wrapper);
		if (documents.isEmpty()) {
			return Collections.emptySet();
		}
		return documents.stream().map(Document::getId).collect(Collectors.toSet());
	}
}
