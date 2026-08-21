package com.stockresearch.copilot.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Unified access log: method, path, status, duration, with TraceId from MDC.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		long started = System.currentTimeMillis();
		try {
			filterChain.doFilter(request, response);
		}
		finally {
			String path = request.getRequestURI();
			if (shouldSkip(path)) {
				return;
			}
			long latencyMs = System.currentTimeMillis() - started;
			log.info("http method={} path={} status={} latencyMs={} query={}",
					request.getMethod(),
					path,
					response.getStatus(),
					latencyMs,
					request.getQueryString() == null ? "" : request.getQueryString());
		}
	}

	private boolean shouldSkip(String path) {
		return path.startsWith("/actuator")
				|| path.startsWith("/v3/api-docs")
				|| path.startsWith("/swagger")
				|| path.startsWith("/doc.html")
				|| path.startsWith("/webjars");
	}
}
