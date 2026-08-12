package com.stockresearch.copilot.controller;

import com.stockresearch.copilot.common.result.ApiResponse;
import com.stockresearch.copilot.common.result.PageResult;
import com.stockresearch.copilot.dto.DocumentQueryRequest;
import com.stockresearch.copilot.service.DocumentService;
import com.stockresearch.copilot.vo.DocumentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Tag(name = "Documents")
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

	private final DocumentService documentService;

	@Operation(summary = "Upload a research document")
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<DocumentVO> upload(
			@RequestParam("companyId") Long companyId,
			@RequestParam("docType") String docType,
			@RequestParam(value = "title", required = false) String title,
			@RequestParam(value = "publishDate", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate publishDate,
			@RequestPart("file") MultipartFile file) {
		return ApiResponse.ok(documentService.upload(companyId, docType, title, publishDate, file));
	}

	@Operation(summary = "Get document by id")
	@GetMapping("/{id}")
	public ApiResponse<DocumentVO> getById(@PathVariable Long id) {
		return ApiResponse.ok(documentService.getById(id));
	}

	@Operation(summary = "Page documents")
	@GetMapping
	public ApiResponse<PageResult<DocumentVO>> page(@Valid DocumentQueryRequest request) {
		return ApiResponse.ok(documentService.page(request));
	}

	@Operation(summary = "Reprocess a document")
	@PostMapping("/{id}/reprocess")
	public ApiResponse<DocumentVO> reprocess(@PathVariable Long id) {
		return ApiResponse.ok(documentService.reprocess(id));
	}
}
