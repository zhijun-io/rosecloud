package io.rosecloud.starter.tenant.web;

import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that resolves the tenant id from the <em>authenticated principal</em>
 * (not from any client-supplied header), binds it to {@link TenantContextHolder} for the
 * request, and clears it afterwards.
 *
 * <p>The tenant is derived from {@link SecurityUser#getTenantId()} so a caller cannot
 * cross tenant boundaries by spoofing an {@code X-Tenant-Id} header. This filter must run
 * <em>after</em> the Spring Security filter chain has populated the security context; see
 * {@link io.rosecloud.starter.tenant.TenantAutoConfiguration}.
 */
public class TenantWebFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String tenantId = resolveTenantId();
        try {
            if (tenantId != null) {
                TenantContextHolder.setTenantId(tenantId);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private static String resolveTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof SecurityUser securityUser) {
            String tenantId = securityUser.getTenantId();
            if (tenantId != null && !tenantId.isBlank()) {
                return tenantId.trim();
            }
        }
        return null;
    }
}
