package com.stockresearch.copilot.controller;

import com.stockresearch.copilot.common.result.ApiResponse;
import com.stockresearch.copilot.dto.AgentRequest;
import com.stockresearch.copilot.service.AgentService;
import com.stockresearch.copilot.vo.AgentAnswerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Agent")
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

	private final AgentService agentService;

	@Operation(summary = "Route a research question to QA or summary tools")
	@PostMapping("/run")
	public ApiResponse<AgentAnswerVO> run(@Valid @RequestBody AgentRequest request) {
		return ApiResponse.ok(agentService.run(request));
	}
}
