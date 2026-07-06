package io.rosecloud.starter.security.jwt;

import java.time.Instant;

/**
 * Tracks revoked (logged-out) tokens by {@code jti} until their expiry, so a
 * stateless JWT can be invalidated before it expires. The auth service revokes
 * on logout; the gateway / monolith filter reject revoked tokens. The default
 * in-memory implementation suits single-instance (monolith) deployments; a
 * shared store (e.g. Redis) is required for multi-instance revocation.
 */
public interface TokenRevocationService {

    /** Marks the token identified by {@code jti} revoked until {@code expiresAt}. */
    void revoke(String jti, Instant expiresAt);

    /** Returns true if the token is currently revoked (and not yet expired out). */
    boolean isRevoked(String jti);
}
