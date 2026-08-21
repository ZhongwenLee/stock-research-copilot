package com.stockresearch.copilot.service.impl;

import com.stockresearch.copilot.common.enums.AgentTool;
import com.stockresearch.copilot.common.enums.IntentType;
import com.stockresearch.copilot.dto.AgentRequest;
import com.stockresearch.copilot.rag.intent.IntentRecognizer;
import com.stockresearch.copilot.rag.intent.QuestionIntent;
import com.stockresearch.copilot.service.QaService;
import com.stockresearch.copilot.service.SummaryService;
import com.stockresearch.copilot.vo.AgentAnswerVO;
import com.stockresearch.copilot.vo.QaAnswerVO;
import com.stockresearch.copilot.vo.SummaryAnswerVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentServiceImplTest {

	@Mock
	private IntentRecognizer intentRecognizer;
	@Mock
	private QaService qaService;
	@Mock
	private SummaryService summaryService;

	private AgentServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new AgentServiceImpl(intentRecognizer, qaService, summaryService);
	}

	@Test
	void routesSummaryIntentToSummaryTool() {
		when(intentRecognizer.recognize(anyString(), any(), any(), any())).thenReturn(QuestionIntent.builder()
				.intentType(IntentType.SUMMARY)
				.companyId(1L)
				.companyName("贵州茅台")
				.stockCode("600519")
				.rawQuestion("请做一份摘要")
				.build());
		when(summaryService.generate(any())).thenReturn(SummaryAnswerVO.builder()
				.companyId(1L)
				.companyName("贵州茅台")
				.stockCode("600519")
				.title("贵州茅台 研究摘要")
				.overview("概况良好")
				.docTypes(List.of())
				.citations(List.of())
				.chunks(List.of())
				.latencyMs(12L)
				.insufficientEvidence(false)
				.build());

		AgentRequest request = new AgentRequest();
		request.setQuestion("请做一份摘要");
		request.setCompanyId(1L);
		AgentAnswerVO answer = service.run(request);

		assertEquals(IntentType.SUMMARY.name(), answer.getIntentType());
		assertEquals(AgentTool.SUMMARY, answer.getSteps().get(1).getTool());
		verify(summaryService).generate(any());
	}

	@Test
	void routesQaIntentToQaTool() {
		when(intentRecognizer.recognize(anyString(), any(), any(), any())).thenReturn(QuestionIntent.builder()
				.intentType(IntentType.QA)
				.companyId(1L)
				.companyName("贵州茅台")
				.rawQuestion("最新营收是多少")
				.build());
		when(qaService.ask(any())).thenReturn(QaAnswerVO.builder()
				.intentType(IntentType.QA.name())
				.question("最新营收是多少")
				.answer("营收 100 亿")
				.companyId(1L)
				.companyName("贵州茅台")
				.preferredDocTypes(List.of())
				.citations(List.of())
				.chunks(List.of())
				.latencyMs(8L)
				.insufficientEvidence(false)
				.build());

		AgentRequest request = new AgentRequest();
		request.setQuestion("最新营收是多少");
		AgentAnswerVO answer = service.run(request);

		assertEquals(IntentType.QA.name(), answer.getIntentType());
		assertEquals(AgentTool.QA, answer.getSteps().get(1).getTool());
		verify(qaService).ask(any());
	}
}
