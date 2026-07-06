package io.rosecloud.starter.tenant.resolver;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the tenant id from an inbound servlet request.
 *
 * <p>Implement and register a bean to override the default header-based
 * resolver (e.g. domain or token based).
 */
public interface TenantResolver {

    /** @return the resolved tenant id, or {@code null} if none could be determined */
    Long resolve(HttpServletRequest request);
}
