package io.rosecloud.starter.security.jwt;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link TokenRevocationService}: a {@code jti -> expiry} map with
 * lazy eviction on read. Single-instance only (monolith); use a shared store
 * for multi-instance revocation. Expired entries are also dropped on access, so
 * the set stays bounded by live revocations.
 */
public class InMemoryTokenRevocationService implements TokenRevocationService {

    private static final Instant FAR_FUTURE = Instant.now().plusSeconds(3600);

    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

    @Override
    public void revoke(String jti, Instant expiresAt) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        revoked.put(jti, expiresAt == null ? FAR_FUTURE : expiresAt);
    }

    @Override
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        Instant expiresAt = revoked.get(jti);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            revoked.remove(jti);
            return false;
        }
        return true;
    }
}
