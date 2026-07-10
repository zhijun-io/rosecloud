package io.rosecloud.api.user;

public record TenantStatusView(
        String tenantId,
        String tenantStatus
) {
}
