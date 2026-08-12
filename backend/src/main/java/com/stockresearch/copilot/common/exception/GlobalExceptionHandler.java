package com.stockresearch.copilot.common.exception;

import com.stockresearch.copilot.common.result.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Global exception translation into unified API responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BizException.class)
	public ApiResponse<Void> handleBizException(BizException ex, HttpServletRequest request) {
		log.warn("biz error path={} code={} message={}", request.getRequestURI(), ex.getErrorCode().getCode(),
				ex.getMessage());
		return withTrace(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
	}

	@ExceptionHandler({
			MethodArgumentNotValidException.class,
			BindException.class,
			ConstraintViolationException.class,
			MethodArgumentTypeMismatchException.class,
			HttpMessageNotReadableException.class,
			IllegalArgumentException.class,
			org.springframework.web.multipart.MaxUploadSizeExceededException.class,
			org.springframework.web.multipart.MultipartException.class
	})
	public ApiResponse<Void> handleValidationException(Exception ex, HttpServletRequest request) {
		String message = resolveValidationMessage(ex);
		log.warn("validation error path={} message={}", request.getRequestURI(), message);
		return withTrace(ApiResponse.fail(ErrorCode.VALIDATION_FAILED, message));
	}

	@ExceptionHandler(Exception.class)
	public ApiResponse<Void> handleException(Exception ex, HttpServletRequest request) {
		log.error("unhandled error path={}", request.getRequestURI(), ex);
		return withTrace(ApiResponse.fail(ErrorCode.INTERNAL_ERROR));
	}

	private String resolveValidationMessage(Exception ex) {
		if (ex instanceof MethodArgumentNotValidException manv && manv.getBindingResult().getFieldError() != null) {
			return manv.getBindingResult().getFieldError().getDefaultMessage();
		}
		if (ex instanceof BindException bind && bind.getBindingResult().getFieldError() != null) {
			return bind.getBindingResult().getFieldError().getDefaultMessage();
		}
		if (ex instanceof ConstraintViolationException cve && !cve.getConstraintViolations().isEmpty()) {
			return cve.getConstraintViolations().iterator().next().getMessage();
		}
		return ErrorCode.VALIDATION_FAILED.getMessage();
	}

	private <T> ApiResponse<T> withTrace(ApiResponse<T> response) {
		response.setTraceId(MDC.get("traceId"));
		return response;
	}
}
