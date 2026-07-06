package io.rosecloud.starter.web.security;

import io.rosecloud.common.security.SecurityHeaders;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Decodes {@link SecurityHeaders} from the inbound request into a
 * {@link CurrentUser} bound to {@link UserContext} for the request duration.
 * Ensures a trace id is always present (reuses the inbound {@code X-Trace-Id},
 * generates one when absent, e.g. direct calls bypassing the gateway), exposes
 * it on the response and in the {@code traceId} MDC key for log correlation.
 */
public class SecurityContextFilter implements Filter {

    /** MDC key holding the current request's trace id. */
    public static final String MDC_TRACE_ID = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String traceId = http.getHeader(SecurityHeaders.TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
        }
        MDC.put(MDC_TRACE_ID, traceId);
        httpResponse.setHeader(SecurityHeaders.TRACE_ID, traceId);
        UserContext.set(decode(http, traceId));
        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
            MDC.remove(MDC_TRACE_ID);
        }
    }

    private CurrentUser decode(HttpServletRequest request, String traceId) {
        Long userId = parseLong(request.getHeader(SecurityHeaders.USER_ID));
        String username = request.getHeader(SecurityHeaders.USERNAME);
        Long tenantId = parseLong(request.getHeader(SecurityHeaders.TENANT_ID));
        List<String> roles = parseRoles(request.getHeader(SecurityHeaders.ROLES));
        return new CurrentUser(userId, username, tenantId, roles, traceId);
    }

    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<String> parseRoles(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
