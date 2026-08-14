package com.stockresearch.copilot.controller;

import com.stockresearch.copilot.common.result.ApiResponse;
import com.stockresearch.copilot.dto.QaAskRequest;
import com.stockresearch.copilot.service.QaService;
import com.stockresearch.copilot.vo.QaAnswerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
