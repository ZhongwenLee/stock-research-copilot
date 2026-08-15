package com.stockresearch.copilot.service;

import com.stockresearch.copilot.dto.SummaryGenerateRequest;
import com.stockresearch.copilot.vo.SummaryAnswerVO;

public interface SummaryService {

	SummaryAnswerVO generate(SummaryGenerateRequest request);
}
