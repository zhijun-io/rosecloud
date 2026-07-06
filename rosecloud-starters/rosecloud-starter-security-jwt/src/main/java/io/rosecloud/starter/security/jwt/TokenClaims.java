package io.rosecloud.starter.security.jwt;

import io.rosecloud.common.security.context.CurrentUser;

import java.util.Collections;
import java.util.List;

/**
 * Decoded JWT claims, aligned with {@link CurrentUser} so the same identity
 * flows end to end: JWT -> gateway headers -> {@code UserContext}.
 */
public record TokenClaims(Long userId, String username, Long tenantId, List<String> roles, TokenType type) {

    public TokenClaims {
        roles = roles == null ? Collections.emptyList() : List.copyOf(roles);
    }
}
