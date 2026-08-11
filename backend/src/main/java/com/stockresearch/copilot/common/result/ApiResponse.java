package com.stockresearch.copilot.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.stockresearch.copilot.common.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * Unified API response body.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> implements Serializable {

	private int code;
	private String message;
	private T data;
	private String traceId;

	public static <T> ApiResponse<T> ok(T data) {
		ApiResponse<T> response = new ApiResponse<>();
		response.setCode(ErrorCode.SUCCESS.getCode());
		response.setMessage(ErrorCode.SUCCESS.getMessage());
		response.setData(data);
		return response;
	}

	public static <T> ApiResponse<T> ok() {
		return ok(null);
	}

	public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
		return fail(errorCode.getCode(), errorCode.getMessage());
	}

	public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
		return fail(errorCode.getCode(), message);
	}

	public static <T> ApiResponse<T> fail(int code, String message) {
		ApiResponse<T> response = new ApiResponse<>();
		response.setCode(code);
		response.setMessage(message);
		return response;
	}
}
