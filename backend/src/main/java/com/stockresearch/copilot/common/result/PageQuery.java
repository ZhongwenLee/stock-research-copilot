package com.stockresearch.copilot.common.result;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Common pagination request.
 */
@Data
public class PageQuery {

	@Min(value = 1, message = "pageNum must be >= 1")
	private long pageNum = 1;

	@Min(value = 1, message = "pageSize must be >= 1")
	@Max(value = 200, message = "pageSize must be <= 200")
	private long pageSize = 20;
}
