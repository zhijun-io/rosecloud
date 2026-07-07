package io.rosecloud.starter.trace.web;

import io.rosecloud.common.security.SecurityHeaders;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.UUID;

/**
 * Generates a new trace id for every servlet request, exposes it on the
 * response, and makes it visible to downstream filters through the request
 * header.
 */
public class TraceContextFilter implements Filter {

    public static final String MDC_TRACE_ID = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String traceId = generateTraceId();
        MDC.put(MDC_TRACE_ID, traceId);
        httpResponse.setHeader(SecurityHeaders.TRACE_ID, traceId);
        try {
            chain.doFilter(new TraceIdRequestWrapper(http, traceId), response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }

    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static final class TraceIdRequestWrapper extends HttpServletRequestWrapper {

        private final String traceId;

        TraceIdRequestWrapper(HttpServletRequest request, String traceId) {
            super(request);
            this.traceId = traceId;
        }

        @Override
        public String getHeader(String name) {
            if (SecurityHeaders.TRACE_ID.equalsIgnoreCase(name)) {
                return traceId;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (SecurityHeaders.TRACE_ID.equalsIgnoreCase(name)) {
                return Collections.enumeration(java.util.List.of(traceId));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            LinkedHashSet<String> names = new LinkedHashSet<>();
            Enumeration<String> superNames = super.getHeaderNames();
            while (superNames.hasMoreElements()) {
                names.add(superNames.nextElement());
            }
            names.add(SecurityHeaders.TRACE_ID);
            return Collections.enumeration(names);
        }
    }
}
