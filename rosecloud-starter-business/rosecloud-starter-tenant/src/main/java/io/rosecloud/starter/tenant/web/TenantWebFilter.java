package io.rosecloud.starter.tenant.web;

import io.rosecloud.common.security.SecurityHeaders;
import io.rosecloud.starter.tenant.core.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that resolves the tenant id from the {@code X-Tenant-Id} header,
 * binds it to {@link TenantContext} for the request, and clears it afterwards.
 */
public class TenantWebFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String tenantId = parse(request);
        try {
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private static String parse(HttpServletRequest request) {
        String value = request.getHeader(SecurityHeaders.TENANT_ID);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
