package io.rosecloud.api.tenant;

public record TenantSummary(Long tenantId, String tenantCode, String tenantName, boolean enabled) {
}

