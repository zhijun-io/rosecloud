package io.rosecloud.starter.security.jwt;

import java.time.Instant;

/**
 * Minimal decoded JWT claims. The token carries only the unique username plus
 * token metadata; servlet services hydrate the full {@code CurrentUser} after
 * validation.
 */
public record TokenClaims(String username, TokenType type, String jti, Instant expiresAt) {
}
