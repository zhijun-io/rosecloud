package io.rosecloud.api.user;

/**
 * Tenant summary exposed to the auth service for login-time tenant selection
 * and tenant switching.
 */
public record TenantAccessCandidate(
        String tenantId,
        String tenantName,
        String tenantStatus,
        boolean selectable
) {
}
