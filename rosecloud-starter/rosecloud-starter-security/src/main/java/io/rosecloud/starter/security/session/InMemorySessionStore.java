package io.rosecloud.starter.security.session;

import io.rosecloud.common.security.model.LoginSession;
import io.rosecloud.common.security.session.SessionStore;
import io.rosecloud.common.security.token.JwtClaimsExtractor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory {@link SessionStore}.
 *
 * <p>Token validity is decoupled from session presence: a token is revoked only
 * when its {@code jti} is in the revocation set (see {@link JwtClaimsExtractor}),
 * so a valid token that was never saved here is still accepted. {@link #save(LoginSession)}
 * exists only for administrative visibility and for cascading a single token's
 * revocation to its paired token (access+refresh of one session).
 *
 * <p>Non-persistent; all data — including the revocation set — is lost on restart,
 * so revocation does NOT propagate across services. Prefer
 * {@link io.rosecloud.starter.security.session.RedisSessionStore} in any deployment
 * with more than one instance or service.
 *
 * <p>Expired entries are lazily purged on each operation so the maps stay bounded.
 */
public class InMemorySessionStore implements SessionStore {

    private final ConcurrentMap<String, LoginSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Instant> revokedJti = new ConcurrentHashMap<>();

    @Override
    public void save(LoginSession session) {
        sessions.put(session.id(), session);
    }

    @Override
    public void revoke(String token) {
        evictExpired();
        JwtClaimsExtractor.extract(token).ifPresent(target -> {
            if (target.expiresAt() != null) {
                revokedJti.put(target.jti(), target.expiresAt());
            }
        });
        // Cascade to the paired token so logout/rotation invalidates the whole session.
        for (LoginSession session : sessions.values()) {
            if (tokenEquals(session, token)) {
                revokeSessionTokens(session);
                sessions.remove(session.id());
            }
        }
    }

    @Override
    public void revokeBySessionId(String sessionId) {
        evictExpired();
        LoginSession removed = sessions.remove(sessionId);
        if (removed != null) {
            revokeSessionTokens(removed);
        }
    }

    @Override
    public void revokeByUserId(Long userId) {
        evictExpired();
        Set<String> toRemove = new HashSet<>();
        for (Map.Entry<String, LoginSession> entry : sessions.entrySet()) {
            if (userId.equals(entry.getValue().userId())) {
                revokeSessionTokens(entry.getValue());
                toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(sessions::remove);
    }

    @Override
    public boolean isRevoked(String token) {
        evictExpired();
        return JwtClaimsExtractor.extract(token)
                .map(target -> revokedJti.containsKey(target.jti()))
                .orElse(false);
    }

    private void revokeSessionTokens(LoginSession session) {
        addRevocation(session.token());
        addRevocation(session.refreshToken());
    }

    private void addRevocation(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        JwtClaimsExtractor.extract(token).ifPresent(target -> {
            if (target.expiresAt() != null) {
                revokedJti.put(target.jti(), target.expiresAt());
            }
        });
    }

    private static boolean tokenEquals(LoginSession session, String token) {
        return token != null && (token.equals(session.token())
                || (session.refreshToken() != null && token.equals(session.refreshToken())));
    }

    private void evictExpired() {
        Instant now = Instant.now();
        revokedJti.values().removeIf(exp -> exp.isBefore(now));
        sessions.values().removeIf(s -> s.expireAt() != null && now.isAfter(s.expireAt()));
    }
}
