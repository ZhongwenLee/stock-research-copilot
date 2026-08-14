package com.stockresearch.copilot.rag.intent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stockresearch.copilot.common.enums.DocType;
import com.stockresearch.copilot.common.enums.IntentType;
import com.stockresearch.copilot.entity.Company;
import com.stockresearch.copilot.mapper.CompanyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class IntentRecognizer {

	private static final Pattern STOCK_CODE_PATTERN = Pattern.compile("(?<!\\d)(\\d{6})(?!\\d)");

	private final CompanyMapper companyMapper;

	public QuestionIntent recognize(String question, Long companyIdHint, String stockCodeHint, List<String> docTypeHints) {
		String text = question == null ? "" : question.trim();
		IntentType intentType = detectIntentType(text);
		List<DocType> docTypes = resolveDocTypes(text, docTypeHints);

		Company company = null;
		if (companyIdHint != null) {
			company = companyMapper.selectById(companyIdHint);
		}
		if (company == null && StringUtils.hasText(stockCodeHint)) {
			company = findByStockCode(stockCodeHint.trim());
		}
		if (company == null) {
			company = resolveCompanyFromText(text).orElse(null);
		}

		return QuestionIntent.builder()
				.intentType(intentType)
				.companyId(company == null ? null : company.getId())
				.companyName(company == null ? null : company.getName())
				.stockCode(company == null ? null : company.getStockCode())
				.preferredDocTypes(docTypes)
				.rawQuestion(text)
				.build();
	}

	private IntentType detectIntentType(String text) {
		String lower = text.toLowerCase(Locale.ROOT);
		if (containsAny(text, "对比", "比较", "分歧", "异同", "vs", "versus")
				|| lower.contains("compare") || lower.contains("versus")) {
			return IntentType.COMPARE;
		}
		if (containsAny(text, "摘要", "总结", "概括", "综述", "研究备忘", "一键摘要")
				|| lower.contains("summary") || lower.contains("summarize")) {
			return IntentType.SUMMARY;
		}
		if (containsAny(text, "帮我查", "调用工具", "自动分析", "agent")) {
			return IntentType.AGENT;
		}
		return IntentType.QA;
	}

	private List<DocType> resolveDocTypes(String text, List<String> hints) {
		LinkedHashSet<DocType> types = new LinkedHashSet<>();
		if (hints != null) {
			for (String hint : hints) {
				if (StringUtils.hasText(hint)) {
					types.add(DocType.from(hint));
				}
			}
		}
		if (!types.isEmpty()) {
			return new ArrayList<>(types);
		}
		if (containsAny(text, "财报", "年报", "季报", "半年报", "业绩报告")) {
			types.add(DocType.FINANCIAL_REPORT);
		}
		if (containsAny(text, "公告", "披露", "提示性公告")) {
			types.add(DocType.ANNOUNCEMENT);
		}
		if (containsAny(text, "研报", "券商", "机构观点", "分析师")) {
			types.add(DocType.RESEARCH_REPORT);
		}
		return new ArrayList<>(types);
	}

	private Optional<Company> resolveCompanyFromText(String text) {
		Matcher matcher = STOCK_CODE_PATTERN.matcher(text);
		if (matcher.find()) {
			Company byCode = findByStockCode(matcher.group(1));
			if (byCode != null) {
				return Optional.of(byCode);
			}
		}
		List<Company> companies = companyMapper.selectList(new LambdaQueryWrapper<Company>()
				.eq(Company::getStatus, "ACTIVE")
				.orderByAsc(Company::getId));
		Company best = null;
		int bestLen = 0;
		for (Company company : companies) {
			if (StringUtils.hasText(company.getName()) && text.contains(company.getName())
					&& company.getName().length() > bestLen) {
				best = company;
				bestLen = company.getName().length();
			}
		}
		return Optional.ofNullable(best);
	}

	private Company findByStockCode(String stockCode) {
		return companyMapper.selectOne(new LambdaQueryWrapper<Company>()
				.eq(Company::getStockCode, stockCode)
				.last("LIMIT 1"));
	}

	private boolean containsAny(String text, String... keywords) {
		for (String keyword : keywords) {
			if (text.contains(keyword)) {
				return true;
			}
		}
		return false;
	}
}
