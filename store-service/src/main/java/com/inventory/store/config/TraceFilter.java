package com.inventory.store.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceFilter extends OncePerRequestFilter {
	private static final Logger log = LoggerFactory.getLogger(TraceFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		long start = System.currentTimeMillis();
		String incomingTraceId = request.getHeader("X-Trace-Id");
		String traceId = (incomingTraceId == null || incomingTraceId.isBlank()) ? UUID.randomUUID().toString() : incomingTraceId;
		MDC.put("traceId", traceId);
		response.setHeader("X-Trace-Id", traceId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			long durationMs = System.currentTimeMillis() - start;
			log.info("method={} path={} status={} durationMs={}", request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
			MDC.remove("traceId");
		}
	}
}

