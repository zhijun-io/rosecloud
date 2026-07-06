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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Decodes {@link SecurityHeaders} from the inbound request into a
 * {@link CurrentUser} bound to {@link UserContext} for the request duration.
 */
public class SecurityContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        UserContext.set(decode(http));
        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private CurrentUser decode(HttpServletRequest request) {
        Long userId = parseLong(request.getHeader(SecurityHeaders.USER_ID));
        String username = request.getHeader(SecurityHeaders.USERNAME);
        Long tenantId = parseLong(request.getHeader(SecurityHeaders.TENANT_ID));
        List<String> roles = parseRoles(request.getHeader(SecurityHeaders.ROLES));
        String traceId = request.getHeader(SecurityHeaders.TRACE_ID);
        return new CurrentUser(userId, username, tenantId, roles, traceId);
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
