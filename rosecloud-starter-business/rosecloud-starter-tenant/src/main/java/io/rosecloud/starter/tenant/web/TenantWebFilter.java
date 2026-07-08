package io.rosecloud.starter.tenant.web;

import io.rosecloud.starter.tenant.core.TenantContext;
import io.rosecloud.starter.tenant.resolver.TenantResolver;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * Servlet filter that resolves the tenant id, binds it to {@link TenantContext}
 * for the request, and clears it afterwards.
 */
public class TenantWebFilter implements Filter {

    private final TenantResolver resolver;

    public TenantWebFilter(TenantResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String tenantId = resolver.resolve((HttpServletRequest) request);
        try {
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
