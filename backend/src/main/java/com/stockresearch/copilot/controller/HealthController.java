package com.stockresearch.copilot.controller;

import com.stockresearch.copilot.common.result.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@Tag(name = "Health")
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

	@Operation(summary = "Health check")
	@GetMapping
	public ApiResponse<Map<String, Object>> health() {
		return ApiResponse.ok(Map.of(
				"status", "UP",
				"service", "stock-research-copilot-backend",
				"timestamp", Instant.now().toString()));
	}
}
