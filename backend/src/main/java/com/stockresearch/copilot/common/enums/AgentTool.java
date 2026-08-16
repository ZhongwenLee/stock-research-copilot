package com.stockresearch.copilot.common.enums;

import java.util.Arrays;

public enum AgentTool {
	QA("研究问答", "基于RAG回答研究问题"),
	SUMMARY("研究摘要", "生成结构化研究摘要"),
	DOCUMENT_SEARCH("文档检索", "按公司/类型/时间检索知识片段"),
	COMPANY_LOOKUP("公司查询", "查询公司基本信息");

	private final String displayName;
	private final String description;

	AgentTool(String displayName, String description) {
		this.displayName = displayName;
		this.description = description;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getDescription() {
		return description;
	}

	public static AgentTool from(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("agentTool is required");
		}
		return Arrays.stream(values())
				.filter(item -> item.name().equalsIgnoreCase(value.trim()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("unsupported agentTool: " + value));
	}
}
