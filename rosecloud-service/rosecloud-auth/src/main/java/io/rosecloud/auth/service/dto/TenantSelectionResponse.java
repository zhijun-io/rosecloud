package io.rosecloud.auth.service.dto;

import io.rosecloud.api.user.TenantAccessCandidate;

import java.util.List;

public record TenantSelectionResponse(
        String currentTenantId,
        String rememberedTenantId,
        List<TenantAccessCandidate> switchableTenants
) {
}
