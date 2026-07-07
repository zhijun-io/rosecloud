package io.rosecloud.common.security;

public final class SecurityHeaders {

    public static final String TENANT_ID = "X-Tenant-Id";

    /**
     * Shared secret presented by RoseCloud services when calling each other's
     * {@code /internal/**} endpoints. Never forwarded to clients; without a
     * matching value an internal endpoint rejects the request with 401 so the
     * password hash and other sensitive snapshots are not exposed externally.
     */
    public static final String INTERNAL_API_KEY = "X-RoseCloud-Internal-Key";

    private SecurityHeaders() {
    }
}
