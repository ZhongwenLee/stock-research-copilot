package com.stockresearch.copilot.service;

import com.stockresearch.copilot.common.result.PageResult;
import com.stockresearch.copilot.dto.ChunkQueryRequest;
import com.stockresearch.copilot.vo.DocumentChunkVO;

public interface DocumentChunkQueryService {

	DocumentChunkVO getById(Long id);

	PageResult<DocumentChunkVO> page(ChunkQueryRequest request);
}
