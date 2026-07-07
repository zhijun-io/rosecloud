package io.rosecloud.common.security.context;

import java.util.Collections;
import java.util.List;

/**
 * Inbound caller identity decoded from bearer JWTs.
 */
public record CurrentUser(Long userId, String username, Long tenantId, List<String> roles,
                           List<String> perms) {

    public CurrentUser {
        roles = roles == null ? Collections.emptyList() : List.copyOf(roles);
        perms = perms == null ? Collections.emptyList() : List.copyOf(perms);
    }
}
