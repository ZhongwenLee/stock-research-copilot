package com.stockresearch.copilot.common.enums;

import com.stockresearch.copilot.common.exception.BizException;
import com.stockresearch.copilot.common.exception.ErrorCode;

import java.util.Arrays;

public enum DocType {
	FINANCIAL_REPORT,
	ANNOUNCEMENT,
	RESEARCH_REPORT;

	public static DocType from(String value) {
		if (value == null || value.isBlank()) {
			throw new BizException(ErrorCode.VALIDATION_FAILED, "docType is required");
		}
		return Arrays.stream(values())
				.filter(item -> item.name().equalsIgnoreCase(value.trim()))
				.findFirst()
				.orElseThrow(() -> new BizException(ErrorCode.VALIDATION_FAILED,
						"unsupported docType: " + value));
	}
}
