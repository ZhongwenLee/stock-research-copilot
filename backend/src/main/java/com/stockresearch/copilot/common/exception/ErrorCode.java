package com.stockresearch.copilot.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Business and system error codes.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	SUCCESS(0, "success"),

	BAD_REQUEST(40000, "bad request"),
	VALIDATION_FAILED(40001, "validation failed"),
	UNAUTHORIZED(40100, "unauthorized"),
	FORBIDDEN(40300, "forbidden"),
	NOT_FOUND(40400, "resource not found"),

	INTERNAL_ERROR(50000, "internal server error"),
	AI_UNAVAILABLE(50301, "ai service unavailable"),
	RETRIEVAL_FAILED(50302, "retrieval failed"),
	INGEST_FAILED(50303, "document ingest failed"),
	SERVICE_UNAVAILABLE(50300, "service unavailable");

	private final int code;
	private final String message;
}
