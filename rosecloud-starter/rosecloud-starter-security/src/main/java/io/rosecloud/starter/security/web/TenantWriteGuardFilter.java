package io.rosecloud.starter.security.web;
import lombok.RequiredArgsConstructor;

import io.rosecloud.api.user.TenantLookupApi;
import io.rosecloud.api.user.TenantStatusView;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Enforces the tenant-write boundary documented in {@code TenantStatusChecks}: a
 * {@code STOPPED} or {@code EXPIRED} tenant is allowed to keep its existing (read-only)
 * session, but must not perform mutating operations. Authentication only validates the
 * token; this filter is the "service/permission layer" guard that blocks writes.
 *
 * <p>The guard is intentionally narrow: it only acts on mutating HTTP methods for a
 * non-system tenant whose status it can actually resolve. Requests without a
 * {@link SecurityUser} principal (e.g. internal {@code ROLE_INTERNAL} machine calls,
 * unauthenticated traffic) fall through to the normal security chain.
 */
@RequiredArgsConstructor
public class TenantWriteGuardFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final String STOPPED = "STOPPED";
    private static final String EXPIRED = "EXPIRED";

    private final ObjectProvider<TenantLookupApi> tenantLookupApiProvider;
    private final boolean failClosed;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SAFE_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Impersonation sessions are strictly read-only.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof SecurityUser securityUser
                && securityUser.isImpersonation()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Impersonation session is read-only");
            return;
        }

        String tenantId = resolveTenantId();
        if (tenantId == null || TenantContextHolder.SYSTEM_TENANT_ID.equals(tenantId)) {
            filterChain.doFilter(request, response);
            return;
        }

        TenantLookupApi tenantLookupApi = tenantLookupApiProvider.getIfAvailable();
        if (tenantLookupApi == null) {
            // M5: no user API available to resolve tenant status. Fail-open by default (the
            // original behaviour for services without a user API); fail-closed blocks the write.
            if (failClosed) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant status unavailable");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        ApiResponse<TenantStatusView> apiResponse = tenantLookupApi.findTenantStatus(tenantId);
        if (apiResponse == null || !apiResponse.success() || apiResponse.data() == null) {
            // M5: could not resolve tenant status (lookup failed / empty). Same fail-open vs
            // fail-closed decision as above.
            if (failClosed) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant status unavailable");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        String status = apiResponse.data().tenantStatus();
        if (STOPPED.equalsIgnoreCase(status) || EXPIRED.equalsIgnoreCase(status)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant write suspended");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Nullable
    private String resolveTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof SecurityUser securityUser)) {
            return null;
        }
        String tenantId = securityUser.getTenantId();
        return StringUtils.hasText(tenantId) ? tenantId : null;
    }
}
