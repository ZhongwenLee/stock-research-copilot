package com.stockresearch.copilot.rag.context;

import com.stockresearch.copilot.config.AppProperties;
import com.stockresearch.copilot.common.enums.SummaryMode;
import com.stockresearch.copilot.entity.DocumentChunk;
import com.stockresearch.copilot.rag.intent.QuestionIntent;
import com.stockresearch.copilot.rag.retrieve.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummaryContextBuilder {

	private final AppProperties appProperties;

	public SummaryContext build(QuestionIntent intent, List<RetrievedChunk> rankedChunks, SummaryMode mode) {
		SummaryTemplate template = SummaryTemplate.of(mode);
		int maxChars = Math.max(1200, mode == SummaryMode.DEEP
				? appProperties.getQa().getMaxContextChars() * 2
				: appProperties.getQa().getMaxContextChars());
		int maxChunks = mode == SummaryMode.DEEP
				? Math.max(appProperties.getQa().getRerankTopK(), 12)
				: Math.max(4, appProperties.getQa().getRerankTopK());

		List<RetrievedChunk> used = new ArrayList<>();
		StringBuilder context = new StringBuilder();
		int chars = 0;
		for (int i = 0; i < rankedChunks.size() && used.size() < maxChunks; i++) {
			RetrievedChunk item = rankedChunks.get(i);
			DocumentChunk chunk = item.getChunk();
			if (chunk == null || !StringUtils.hasText(chunk.getContent())) {
				continue;
			}
			String block = formatChunk(used.size() + 1, chunk);
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

		String companyLine = intent.getCompanyName() == null
				? "未指定公司"
				: intent.getCompanyName() + (intent.getStockCode() == null ? "" : "（" + intent.getStockCode() + "）");

		String userPrompt;
		if (used.isEmpty()) {
			userPrompt = """
					目标公司：%s
					摘要模式：%s
					摘要标题：%s
					摘要章节：%s
					用户问题：%s
					
					当前没有检索到可用资料。请明确说明依据不足，并建议用户补充财报、公告或研报。
					""".formatted(companyLine, template.getMode().name(), template.getTitle(),
					String.join("、", template.getSections()), intent.getRawQuestion());
		}
		else {
			userPrompt = """
					目标公司：%s
					摘要模式：%s
					摘要标题：%s
					摘要章节：%s
					用户问题：%s
					
					输出要求：%s
					
					参考资料：
					%s
					""".formatted(companyLine, template.getMode().name(), template.getTitle(),
					String.join("、", template.getSections()), intent.getRawQuestion(),
					template.getUserInstruction(), context);
		}

		return SummaryContext.builder()
				.template(template)
				.systemPrompt(template.getSystemPrompt())
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
