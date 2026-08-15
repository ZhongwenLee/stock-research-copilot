package com.stockresearch.copilot.rag.context;

import com.stockresearch.copilot.common.enums.SummaryMode;
import lombok.Getter;

import java.util.List;

@Getter
public final class SummaryTemplate {

	private final SummaryMode mode;
	private final String title;
	private final int targetWords;
	private final int maxCitations;
	private final List<String> sections;
	private final String systemPrompt;
	private final String userInstruction;

	private SummaryTemplate(SummaryMode mode, String title, int targetWords, int maxCitations,
						   List<String> sections, String systemPrompt, String userInstruction) {
		this.mode = mode;
		this.title = title;
		this.targetWords = targetWords;
		this.maxCitations = maxCitations;
		this.sections = sections;
		this.systemPrompt = systemPrompt;
		this.userInstruction = userInstruction;
	}

	public static SummaryTemplate fast() {
		return new SummaryTemplate(
				SummaryMode.FAST,
				"极速研究摘要",
				700,
				6,
				List.of("公司概况", "经营变化", "财务指标", "风险", "机构观点", "关注点"),
				"你是股票研究摘要助手。请仅依据给定资料生成结构化摘要，中文输出，关键结论后用 [数字] 标注引用。",
				"输出要求：每个章节 2-4 句，偏向结论和驱动因素，尽量控制在 700 字左右。"
		);
	}

	public static SummaryTemplate deep() {
		return new SummaryTemplate(
				SummaryMode.DEEP,
				"深度研究摘要",
				1800,
				12,
				List.of("公司概况", "经营变化", "财务指标", "风险", "机构观点", "关注点", "后续跟踪"),
				"你是股票研究摘要助手。请仅依据给定资料生成深度研究摘要，中文输出，关键结论后用 [数字] 标注引用。",
				"输出要求：按章节展开，保留主要数据、对比与判断，尽量控制在 1800 字左右。"
		);
	}

	public static SummaryTemplate of(SummaryMode mode) {
		return mode == SummaryMode.DEEP ? deep() : fast();
	}
}
