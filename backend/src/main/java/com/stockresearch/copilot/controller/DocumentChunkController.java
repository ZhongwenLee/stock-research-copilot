package com.stockresearch.copilot.controller;

import com.stockresearch.copilot.common.result.ApiResponse;
import com.stockresearch.copilot.common.result.PageResult;
import com.stockresearch.copilot.dto.ChunkQueryRequest;
import com.stockresearch.copilot.service.DocumentChunkQueryService;
import com.stockresearch.copilot.vo.DocumentChunkVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Document Chunks")
@RestController
@RequestMapping("/api/v1/chunks")
@RequiredArgsConstructor
public class DocumentChunkController {

	private final DocumentChunkQueryService documentChunkQueryService;

	@Operation(summary = "Get chunk by id")
	@GetMapping("/{id}")
	public ApiResponse<DocumentChunkVO> getById(@PathVariable Long id) {
		return ApiResponse.ok(documentChunkQueryService.getById(id));
	}

	@Operation(summary = "Query chunks by document / company / publish date")
	@GetMapping
	public ApiResponse<PageResult<DocumentChunkVO>> page(@Valid ChunkQueryRequest request) {
		return ApiResponse.ok(documentChunkQueryService.page(request));
	}
}
