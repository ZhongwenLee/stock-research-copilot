package com.stockresearch.copilot.service;

import com.stockresearch.copilot.dto.CompanyCreateRequest;
import com.stockresearch.copilot.vo.CompanyVO;

import java.util.List;

public interface CompanyService {

	CompanyVO create(CompanyCreateRequest request);

	CompanyVO getById(Long id);

	List<CompanyVO> listAll();
}
