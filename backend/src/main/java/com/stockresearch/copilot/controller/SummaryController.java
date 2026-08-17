package com.stockresearch.copilot.controller;

import com.stockresearch.copilot.common.result.ApiResponse;
import com.stockresearch.copilot.common.result.PageResult;
import com.stockresearch.copilot.dto.SummaryGenerateRequest;
import com.stockresearch.copilot.dto.SummaryQueryRequest;
import com.stockresearch.copilot.service.SummaryService;
import com.stockresearch.copilot.vo.SummaryAnswerVO;
import com.stockresearch.copilot.vo.SummaryHistoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Summary")
@RestController
@RequestMapping("/api/v1/summary")
@RequiredArgsConstructor
public class SummaryController {

	private final SummaryService summaryService;

	@Operation(summary = "Generate a structured research summary")
	@PostMapping("/generate")
	public ApiResponse<SummaryAnswerVO> generate(@Valid @RequestBody SummaryGenerateRequest request) {
		return ApiResponse.ok(summaryService.generate(request));
	}

	@Operation(summary = "Page summary history")
	@GetMapping("/history")
	public ApiResponse<PageResult<SummaryHistoryVO>> history(@Valid SummaryQueryRequest request) {
		return ApiResponse.ok(summaryService.history(request));
	}

	@Operation(summary = "Get summary history by id")
	@GetMapping("/history/{summaryId}")
	public ApiResponse<SummaryHistoryVO> historyById(@PathVariable Long summaryId) {
		return ApiResponse.ok(summaryService.getHistoryById(summaryId));
	}
}
