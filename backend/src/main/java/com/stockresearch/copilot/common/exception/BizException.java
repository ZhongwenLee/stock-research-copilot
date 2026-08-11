package com.stockresearch.copilot.common.exception;

import lombok.Getter;

/**
 * Business exception carrying a stable error code.
 */
@Getter
public class BizException extends RuntimeException {

	private final ErrorCode errorCode;

	public BizException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public BizException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
