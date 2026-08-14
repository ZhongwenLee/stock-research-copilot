package com.stockresearch.copilot.rag.vector;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class VectorSearchFilter {

	private Long companyId;
	private Set<Long> documentIds;
}
