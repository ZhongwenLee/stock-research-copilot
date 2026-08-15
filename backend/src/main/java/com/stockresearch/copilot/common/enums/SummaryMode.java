package com.stockresearch.copilot.common.enums;

public enum SummaryMode {
	FAST,
	DEEP;

	public static SummaryMode from(String value) {
		if (value == null || value.isBlank()) {
			return FAST;
		}
		for (SummaryMode mode : values()) {
			if (mode.name().equalsIgnoreCase(value.trim())) {
				return mode;
			}
		}
		return FAST;
	}
}
