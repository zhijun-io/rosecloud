package io.rosecloud.starter.security.jwt;

import io.rosecloud.common.security.context.CurrentUser;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Decoded JWT claims, aligned with {@link CurrentUser} so the same identity
 * flows end to end: JWT -> gateway headers -> {@code UserContext}. Carries the
 * token id ({@code jti}) and expiry for revocation tracking.
 */
public record TokenClaims(Long userId, String username, Long tenantId, List<String> roles, TokenType type,
                          String jti, Instant expiresAt) {

    public TokenClaims {
        roles = roles == null ? Collections.emptyList() : List.copyOf(roles);
    }
}
