package com.stockresearch.copilot.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Common pagination response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

	private List<T> records;
	private long total;
	private long pageNum;
	private long pageSize;

	public static <T> PageResult<T> of(List<T> records, long total, long pageNum, long pageSize) {
		return new PageResult<>(records, total, pageNum, pageSize);
	}

	public static <T> PageResult<T> empty(long pageNum, long pageSize) {
		return new PageResult<>(Collections.emptyList(), 0L, pageNum, pageSize);
	}
}
