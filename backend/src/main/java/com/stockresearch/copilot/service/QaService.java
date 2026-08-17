package com.stockresearch.copilot.service;

import com.stockresearch.copilot.common.result.PageResult;
import com.stockresearch.copilot.dto.QaAskRequest;
import com.stockresearch.copilot.dto.QuestionQueryRequest;
import com.stockresearch.copilot.vo.QaAnswerVO;
import com.stockresearch.copilot.vo.QuestionHistoryVO;

public interface QaService {

	QaAnswerVO ask(QaAskRequest request);

	PageResult<QuestionHistoryVO> history(QuestionQueryRequest request);

	QuestionHistoryVO getHistoryById(Long questionId);
}
