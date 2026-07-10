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

    /** Standard HTTP Authorization header. */
    public static final String AUTHORIZATION = "Authorization";

    /** Bearer token prefix, including the trailing space. */
    public static final String BEARER_PREFIX = "Bearer ";

    /** Identifies the calling service on inter-service requests. */
    public static final String SERVICE_NAME = "X-Service-Name";

    private SecurityHeaders() {}
}
