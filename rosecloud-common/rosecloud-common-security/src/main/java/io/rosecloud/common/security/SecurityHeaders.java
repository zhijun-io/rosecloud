package io.rosecloud.common.security;

/**
 * HTTP header names used for cross-service security context propagation.
 *
 * <p>These constants are shared by both servlet services and the reactive
 * gateway, ensuring a single source of truth for header-based contract.
 */
public final class SecurityHeaders {

    /** Header that carries the tenant identifier across service boundaries. */
    public static final String TENANT_ID = "X-Tenant-Id";

    private SecurityHeaders() {}
}
