package com.stockresearch.copilot.rag.intent;

import com.stockresearch.copilot.common.enums.DocType;
import com.stockresearch.copilot.common.enums.IntentType;
import com.stockresearch.copilot.entity.Company;
import com.stockresearch.copilot.mapper.CompanyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntentRecognizerTest {

	@Mock
	private CompanyMapper companyMapper;

	private IntentRecognizer recognizer;

	@BeforeEach
	void setUp() {
		recognizer = new IntentRecognizer(companyMapper);
	}

	@Test
	void detectsSummaryAndFinancialReport() {
		Company company = new Company();
		company.setId(1L);
		company.setStockCode("600519");
		company.setName("贵州茅台");
		company.setStatus("ACTIVE");
		when(companyMapper.selectList(any())).thenReturn(List.of(company));

		QuestionIntent intent = recognizer.recognize("请对贵州茅台财报做一份摘要", null, null, null);

		assertEquals(IntentType.SUMMARY, intent.getIntentType());
		assertEquals(1L, intent.getCompanyId());
		assertTrue(intent.getPreferredDocTypes().contains(DocType.FINANCIAL_REPORT));
	}

	@Test
	void detectsCompareIntent() {
		when(companyMapper.selectList(any())).thenReturn(List.of());
		QuestionIntent intent = recognizer.recognize("对比两篇研报的分歧点", null, null, null);
		assertEquals(IntentType.COMPARE, intent.getIntentType());
		assertTrue(intent.getPreferredDocTypes().contains(DocType.RESEARCH_REPORT));
	}
}
