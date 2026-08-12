package com.stockresearch.copilot.service;

import com.stockresearch.copilot.common.result.PageResult;
import com.stockresearch.copilot.dto.DocumentQueryRequest;
import com.stockresearch.copilot.vo.DocumentVO;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public interface DocumentService {

	DocumentVO upload(Long companyId, String docType, String title, LocalDate publishDate, MultipartFile file);

	DocumentVO getById(Long id);

	PageResult<DocumentVO> page(DocumentQueryRequest request);

	DocumentVO reprocess(Long id);
}
