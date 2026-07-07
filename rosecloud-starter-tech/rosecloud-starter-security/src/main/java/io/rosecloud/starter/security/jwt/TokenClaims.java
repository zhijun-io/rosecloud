package io.rosecloud.starter.security.jwt;

import java.time.Instant;
import java.util.List;

/**
 * Minimal decoded JWT claims. The access token carries the unique username,
 * fine-grained permission codes ({@code perms}) and token metadata; servlet
 * services hydrate the full {@code CurrentUser} (roles resolved from the system
 * user source) after validation.
 */
public record TokenClaims(String username, TokenType type, String jti, Instant expiresAt,
                          List<String> perms) {
    public TokenClaims {
        perms = perms == null ? List.of() : List.copyOf(perms);
    }
}
