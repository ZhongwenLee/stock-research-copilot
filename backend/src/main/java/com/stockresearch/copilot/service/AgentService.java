package com.stockresearch.copilot.service;

import com.stockresearch.copilot.dto.AgentRequest;
import com.stockresearch.copilot.vo.AgentAnswerVO;

public interface AgentService {

	AgentAnswerVO run(AgentRequest request);
}
