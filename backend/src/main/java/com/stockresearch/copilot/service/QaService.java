package com.stockresearch.copilot.service;

import com.stockresearch.copilot.dto.QaAskRequest;
import com.stockresearch.copilot.vo.QaAnswerVO;

public interface QaService {

	QaAnswerVO ask(QaAskRequest request);
}
