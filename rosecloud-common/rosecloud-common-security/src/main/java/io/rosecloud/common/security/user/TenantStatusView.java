package io.rosecloud.common.security.user;

public record TenantStatusView(
        String tenantId,
        String tenantStatus
) {
}
