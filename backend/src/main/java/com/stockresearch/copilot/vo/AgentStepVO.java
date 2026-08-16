package com.stockresearch.copilot.vo;

import com.stockresearch.copilot.common.enums.AgentStepStatus;
import com.stockresearch.copilot.common.enums.AgentTool;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentStepVO {

	private String name;
	private AgentTool tool;
	private AgentStepStatus status;
	private String note;
	private Long latencyMs;
}
