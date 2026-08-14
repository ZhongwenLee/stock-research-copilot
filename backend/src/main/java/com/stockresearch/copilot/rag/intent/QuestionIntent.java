package com.stockresearch.copilot.rag.intent;

import com.stockresearch.copilot.common.enums.DocType;
import com.stockresearch.copilot.common.enums.IntentType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuestionIntent {

	private IntentType intentType;
	private Long companyId;
	private String companyName;
	private String stockCode;
	private List<DocType> preferredDocTypes;
	private String rawQuestion;
}
