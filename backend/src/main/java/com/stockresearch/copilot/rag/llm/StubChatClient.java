package com.stockresearch.copilot.rag.llm;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StubChatClient implements ChatClient {

	@Override
	public String chat(String systemPrompt, String userPrompt) {
		log.debug("StubChatClient generating answer");
		if (userPrompt != null && userPrompt.contains("没有检索到可用资料")) {
			return "依据不足：当前知识库中没有检索到可支撑该问题的文档片段。请先上传相关财报、公告或研报后再提问。";
		}
		StringBuilder answer = new StringBuilder();
		answer.append("（本地 Stub 模式）根据已检索资料，初步回答如下：\n");
		if (userPrompt != null && userPrompt.contains("用户问题：")) {
			int idx = userPrompt.indexOf("用户问题：");
			int end = userPrompt.indexOf('\n', idx);
			String question = end > idx ? userPrompt.substring(idx + 5, end).trim() : userPrompt.substring(idx + 5).trim();
			answer.append("关于「").append(question).append("」，资料中可见相关表述，请结合引用片段进一步核实。[1]");
		}
		else {
			answer.append("请结合引用来源阅读原文细节。[1]");
		}
		answer.append("\n\n说明：未配置 AI_API_KEY 时使用 Stub 回答，仅用于联调。");
		return answer.toString();
	}
}
