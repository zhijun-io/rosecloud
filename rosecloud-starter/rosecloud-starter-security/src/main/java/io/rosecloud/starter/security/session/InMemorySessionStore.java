package io.rosecloud.starter.security.session;

import io.rosecloud.common.security.model.LoginSession;
import io.rosecloud.common.security.session.SessionStore;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory {@link SessionStore}. Sessions are held in a
 * {@link ConcurrentHashMap} keyed by {@code sessionId}.
 * Non-persistent; all data is lost on restart.
 *
 * <p>Expired sessions are lazily purged on each read/revocation operation so
 * that the map does not grow unboundedly.
 *
 * <p>{@link #isRevoked(String)} iterates the session map to locate
 * the token, so it scales linearly with the number of active sessions.
 * For production with many concurrent sessions prefer
 * {@link io.rosecloud.starter.security.session.RedisSessionStore}.
 */
public class InMemorySessionStore implements SessionStore {

    private final ConcurrentMap<String, LoginSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(LoginSession session) {
        sessions.put(session.id(), session);
    }

    @Override
    public void revoke(String token) {
        evictExpired();
        Set<String> toRemove = new HashSet<>();
        for (Map.Entry<String, LoginSession> entry : sessions.entrySet()) {
            LoginSession session = entry.getValue();
            if (session.token().equals(token)
                    || (session.refreshToken() != null && session.refreshToken().equals(token))) {
                toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(sessions::remove);
    }

    @Override
    public void revokeBySessionId(String sessionId) {
        evictExpired();
        sessions.remove(sessionId);
    }

    @Override
    public void revokeByUserId(Long userId) {
        evictExpired();
        Set<String> toRemove = new HashSet<>();
        for (Map.Entry<String, LoginSession> entry : sessions.entrySet()) {
            if (entry.getValue().userId().equals(userId)) {
                toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(sessions::remove);
    }

    @Override
    public boolean isRevoked(String token) {
        evictExpired();
        return sessions.values().stream().noneMatch(s ->
                s.token().equals(token)
                        || (s.refreshToken() != null && s.refreshToken().equals(token)));
    }

    /**
     * Removes all sessions whose {@code expireAt} is in the past.
     * Called at the start of every revocation / query operation so that
     * the map stays bounded without a background thread.
     */
    private void evictExpired() {
        Instant now = Instant.now();
        sessions.values().removeIf(s -> s.expireAt() != null && now.isAfter(s.expireAt()));
    }
}
