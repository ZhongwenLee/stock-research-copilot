package com.stockresearch.copilot.controller;

import com.stockresearch.copilot.common.result.ApiResponse;
import com.stockresearch.copilot.common.result.PageResult;
import com.stockresearch.copilot.dto.QaAskRequest;
import com.stockresearch.copilot.dto.QuestionQueryRequest;
import com.stockresearch.copilot.service.QaService;
import com.stockresearch.copilot.vo.QaAnswerVO;
import com.stockresearch.copilot.vo.QuestionHistoryVO;
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

@Tag(name = "Q&A")
@RestController
@RequestMapping("/api/v1/qa")
@RequiredArgsConstructor
public class QaController {

	private final QaService qaService;

	@Operation(summary = "Ask a research question with RAG citations")
	@PostMapping("/ask")
	public ApiResponse<QaAnswerVO> ask(@Valid @RequestBody QaAskRequest request) {
		return ApiResponse.ok(qaService.ask(request));
	}

	@Operation(summary = "Page question history")
	@GetMapping("/history")
	public ApiResponse<PageResult<QuestionHistoryVO>> history(@Valid QuestionQueryRequest request) {
		return ApiResponse.ok(qaService.history(request));
	}

	@Operation(summary = "Get question history by id")
	@GetMapping("/history/{questionId}")
	public ApiResponse<QuestionHistoryVO> historyById(@PathVariable Long questionId) {
		return ApiResponse.ok(qaService.getHistoryById(questionId));
	}
}
