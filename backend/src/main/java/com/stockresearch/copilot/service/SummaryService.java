package com.stockresearch.copilot.service;

import com.stockresearch.copilot.common.result.PageResult;
import com.stockresearch.copilot.dto.SummaryGenerateRequest;
import com.stockresearch.copilot.dto.SummaryQueryRequest;
import com.stockresearch.copilot.vo.SummaryAnswerVO;
import com.stockresearch.copilot.vo.SummaryHistoryVO;

public interface SummaryService {

	SummaryAnswerVO generate(SummaryGenerateRequest request);

	PageResult<SummaryHistoryVO> history(SummaryQueryRequest request);

	SummaryHistoryVO getHistoryById(Long summaryId);
}
