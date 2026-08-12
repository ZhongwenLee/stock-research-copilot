package com.stockresearch.copilot.controller;

import com.stockresearch.copilot.common.result.ApiResponse;
import com.stockresearch.copilot.dto.CompanyCreateRequest;
import com.stockresearch.copilot.service.CompanyService;
import com.stockresearch.copilot.vo.CompanyVO;
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

import java.util.List;

@Tag(name = "Companies")
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

	private final CompanyService companyService;

	@Operation(summary = "Create company")
	@PostMapping
	public ApiResponse<CompanyVO> create(@Valid @RequestBody CompanyCreateRequest request) {
		return ApiResponse.ok(companyService.create(request));
	}

	@Operation(summary = "Get company by id")
	@GetMapping("/{id}")
	public ApiResponse<CompanyVO> getById(@PathVariable Long id) {
		return ApiResponse.ok(companyService.getById(id));
	}

	@Operation(summary = "List companies")
	@GetMapping
	public ApiResponse<List<CompanyVO>> list() {
		return ApiResponse.ok(companyService.listAll());
	}
}
