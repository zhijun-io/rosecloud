package io.rosecloud.common.security.context;

import java.util.Collections;
import java.util.List;

/**
 * Inbound caller identity decoded from
 * {@link io.rosecloud.common.security.SecurityHeaders}.
 */
public record CurrentUser(Long userId, String username, Long tenantId, List<String> roles, String traceId) {

    public CurrentUser {
        roles = roles == null ? Collections.emptyList() : List.copyOf(roles);
    }
}
