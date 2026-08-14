package com.stockresearch.copilot.rag.context;

import com.stockresearch.copilot.config.AppProperties;
import com.stockresearch.copilot.entity.DocumentChunk;
import com.stockresearch.copilot.rag.intent.QuestionIntent;
import com.stockresearch.copilot.rag.retrieve.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContextBuilder {

	private final AppProperties appProperties;

	public PromptContext build(QuestionIntent intent, List<RetrievedChunk> rankedChunks) {
		int maxChars = Math.max(1000, appProperties.getQa().getMaxContextChars());
		List<RetrievedChunk> used = new ArrayList<>();
		StringBuilder context = new StringBuilder();
		int chars = 0;

		for (int i = 0; i < rankedChunks.size(); i++) {
			RetrievedChunk item = rankedChunks.get(i);
			DocumentChunk chunk = item.getChunk();
			if (chunk == null || !StringUtils.hasText(chunk.getContent())) {
				continue;
			}
			String block = formatChunk(i + 1, chunk);
			if (chars + block.length() > maxChars && !used.isEmpty()) {
				break;
			}
			if (!context.isEmpty()) {
				context.append("\n\n");
			}
			context.append(block);
			chars += block.length();
			used.add(item);
		}

		String systemPrompt = """
				你是股票研究助手。请仅依据给定资料回答问题。
				要求：
				1. 用中文回答，条理清晰。
				2. 关键结论后用 [数字] 标注引用来源，数字对应资料编号。
				3. 若资料不足以支撑结论，明确说明“依据不足”，不要编造。
				4. 不要输出与问题无关的内容。
				""".stripIndent().trim();

		String companyLine = intent.getCompanyName() == null
				? "未指定公司"
				: intent.getCompanyName() + (intent.getStockCode() == null ? "" : "（" + intent.getStockCode() + "）");

		String userPrompt;
		if (used.isEmpty()) {
			userPrompt = """
					目标公司：%s
					问题类型：%s
					用户问题：%s

					当前没有检索到可用资料。请明确说明依据不足，并建议用户补充财报/公告/研报。
					""".formatted(companyLine, intent.getIntentType().name(), intent.getRawQuestion());
		}
		else {
			userPrompt = """
					目标公司：%s
					问题类型：%s
					用户问题：%s

					参考资料：
					%s
					""".formatted(companyLine, intent.getIntentType().name(), intent.getRawQuestion(), context);
		}

		return PromptContext.builder()
				.systemPrompt(systemPrompt)
				.userPrompt(userPrompt)
				.usedChunks(used)
				.contextChars(chars)
				.build();
	}

	private String formatChunk(int index, DocumentChunk chunk) {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(index).append("]");
		if (StringUtils.hasText(chunk.getTitlePath())) {
			sb.append(" 章节：").append(chunk.getTitlePath());
		}
		if (chunk.getPageNo() != null) {
			sb.append(" 页码：").append(chunk.getPageNo());
		}
		sb.append('\n').append(chunk.getContent().trim());
		return sb.toString();
	}
}
