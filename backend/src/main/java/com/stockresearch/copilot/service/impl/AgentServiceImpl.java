package com.stockresearch.copilot.service.impl;

import com.stockresearch.copilot.common.enums.AgentStepStatus;
import com.stockresearch.copilot.common.enums.AgentTool;
import com.stockresearch.copilot.common.enums.IntentType;
import com.stockresearch.copilot.common.enums.SummaryMode;
import com.stockresearch.copilot.dto.AgentRequest;
import com.stockresearch.copilot.dto.QaAskRequest;
import com.stockresearch.copilot.dto.SummaryGenerateRequest;
import com.stockresearch.copilot.rag.intent.IntentRecognizer;
import com.stockresearch.copilot.rag.intent.QuestionIntent;
import com.stockresearch.copilot.service.AgentService;
import com.stockresearch.copilot.service.QaService;
import com.stockresearch.copilot.service.SummaryService;
import com.stockresearch.copilot.vo.AgentAnswerVO;
import com.stockresearch.copilot.vo.AgentStepVO;
import com.stockresearch.copilot.vo.QaAnswerVO;
import com.stockresearch.copilot.vo.SummaryAnswerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

	private final IntentRecognizer intentRecognizer;
	private final QaService qaService;
	private final SummaryService summaryService;

	@Override
	public AgentAnswerVO run(AgentRequest request) {
		long started = System.currentTimeMillis();
		QuestionIntent intent = intentRecognizer.recognize(
				request.getQuestion(),
				request.getCompanyId(),
				request.getStockCode(),
				request.getDocTypes());

		List<AgentStepVO> steps = new ArrayList<>();
		steps.add(AgentStepVO.builder()
				.name("意图识别")
				.tool(AgentTool.COMPANY_LOOKUP)
				.status(AgentStepStatus.DONE)
				.note(buildIntentNote(intent))
				.latencyMs(0L)
				.build());

		AgentAnswerVO answer;
		if (shouldUseSummary(intent, request)) {
			steps.add(AgentStepVO.builder()
					.name("摘要生成")
					.tool(AgentTool.SUMMARY)
					.status(AgentStepStatus.EXECUTING)
					.note("路由到研究摘要工具")
					.build());

			SummaryGenerateRequest summaryRequest = toSummaryRequest(intent, request);
			SummaryAnswerVO summaryAnswer = summaryService.generate(summaryRequest);
			steps.set(1, AgentStepVO.builder()
					.name("摘要生成")
					.tool(AgentTool.SUMMARY)
					.status(AgentStepStatus.DONE)
					.note(summaryAnswer.getTitle())
					.latencyMs(summaryAnswer.getLatencyMs())
					.build());
			answer = AgentAnswerVO.builder()
					.intentType(IntentType.SUMMARY.name())
					.question(intent.getRawQuestion())
					.answer(summaryAnswer.getOverview())
					.companyId(summaryAnswer.getCompanyId())
					.companyName(summaryAnswer.getCompanyName())
					.stockCode(summaryAnswer.getStockCode())
					.preferredDocTypes(summaryAnswer.getDocTypes())
					.steps(List.copyOf(steps))
					.citations(summaryAnswer.getCitations())
					.chunks(summaryAnswer.getChunks())
					.latencyMs(System.currentTimeMillis() - started)
					.insufficientEvidence(summaryAnswer.isInsufficientEvidence())
					.build();
		}
		else {
			steps.add(AgentStepVO.builder()
					.name("问答检索")
					.tool(AgentTool.QA)
					.status(AgentStepStatus.EXECUTING)
					.note("路由到研究问答工具")
					.build());

			QaAnswerVO qaAnswer = qaService.ask(toQaRequest(intent, request));
			steps.set(1, AgentStepVO.builder()
					.name("问答检索")
					.tool(AgentTool.QA)
					.status(AgentStepStatus.DONE)
					.note(intent.getIntentType().name())
					.latencyMs(qaAnswer.getLatencyMs())
					.build());
			answer = AgentAnswerVO.builder()
					.intentType(qaAnswer.getIntentType())
					.question(qaAnswer.getQuestion())
					.answer(qaAnswer.getAnswer())
					.companyId(qaAnswer.getCompanyId())
					.companyName(qaAnswer.getCompanyName())
					.stockCode(qaAnswer.getStockCode())
					.preferredDocTypes(qaAnswer.getPreferredDocTypes())
					.steps(List.copyOf(steps))
					.citations(qaAnswer.getCitations())
					.chunks(qaAnswer.getChunks())
					.latencyMs(System.currentTimeMillis() - started)
					.insufficientEvidence(qaAnswer.isInsufficientEvidence())
					.build();
		}

		return answer;
	}

	private boolean shouldUseSummary(QuestionIntent intent, AgentRequest request) {
		if (intent.getCompanyId() == null && request.getCompanyId() == null) {
			return false;
		}
		if (intent.getIntentType() == IntentType.SUMMARY) {
			return true;
		}
		String question = intent.getRawQuestion() == null ? "" : intent.getRawQuestion();
		if (StringUtils.hasText(request.getSummaryMode()) && SummaryMode.DEEP.name().equalsIgnoreCase(request.getSummaryMode())) {
			return true;
		}
		return containsAny(question, "摘要", "总结", "概括", "综述", "公司概况", "业绩变化", "风险", "机构观点");
	}

	private SummaryGenerateRequest toSummaryRequest(QuestionIntent intent, AgentRequest request) {
		SummaryGenerateRequest summaryRequest = new SummaryGenerateRequest();
		summaryRequest.setCompanyId(intent.getCompanyId());
		summaryRequest.setStockCode(intent.getStockCode());
		summaryRequest.setDocTypes(request.getDocTypes());
		summaryRequest.setMode(SummaryMode.from(request.getSummaryMode()));
		summaryRequest.setStartDate(request.getStartDate());
		summaryRequest.setEndDate(request.getEndDate());
		summaryRequest.setTopK(request.getTopK());
		return summaryRequest;
	}

	private QaAskRequest toQaRequest(QuestionIntent intent, AgentRequest request) {
		QaAskRequest qaRequest = new QaAskRequest();
		qaRequest.setQuestion(intent.getRawQuestion());
		qaRequest.setCompanyId(intent.getCompanyId());
		qaRequest.setStockCode(intent.getStockCode());
		qaRequest.setDocTypes(request.getDocTypes());
		qaRequest.setTopK(request.getTopK());
		qaRequest.setSummaryMode(request.getSummaryMode());
		qaRequest.setStartDate(normalizeDate(request.getStartDate()));
		qaRequest.setEndDate(normalizeDate(request.getEndDate()));
		return qaRequest;
	}

	private String normalizeDate(LocalDate date) {
		return date == null ? null : date.toString();
	}

	private String buildIntentNote(QuestionIntent intent) {
		String company = intent.getCompanyName() == null ? "未识别公司" : intent.getCompanyName();
		return company + " · " + intent.getIntentType().name();
	}

	private boolean containsAny(String text, String... keywords) {
		for (String keyword : keywords) {
			if (text != null && text.contains(keyword)) {
				return true;
			}
		}
		return false;
	}
}
