package io.rosecloud.common.security.session;

import io.rosecloud.common.security.model.LoginSession;


/**
 * Unified store for login sessions and token revocation state.
 * Serves both the authentication chain (isRevoked checks on every request)
 * and session administration (save on login, revoke on logout/kick).
 *
 * <h2>Validity vs. revocation</h2>
 * A token is accepted when it is signature-valid AND its {@code jti} is NOT in
 * the revocation set. Token validity therefore does <em>not</em> depend on a
 * server-side session record existing — {@link #save(LoginSession)} is for
 * audit / administration only. This keeps validation stateless: a service that
 * shares only the revocation set (e.g. via Redis) can validate tokens after a
 * restart or without ever having saved a session, and revocation propagates
 * consistently across all services.
 *
 * <p>Revocation granularity (every variant adds the affected token {@code jti}s
 * to the revocation set, keyed by {@code jti} with a TTL equal to the token's
 * remaining lifetime):
 * <ul>
 *   <li>{@link #revoke(String)} — revoke the token and its paired token
 *       (access+refresh of the same session)
 *   <li>{@link #revokeBySessionId(String)} — revoke a single session
 *   <li>{@link #revokeByUserId(Long)} — revoke ALL sessions of a user
 * </ul>
 *
 * <p>Implementations must be fast on {@link #isRevoked(String)} — it is called
 * on every request. Prefer a shared (Redis) backing store in multi-service
 * deployments so revocation propagates; an in-memory store only revokes
 * locally and is lost on restart.
 */
public interface SessionStore {

    void save(LoginSession session);

    /**
     * Revoke the token (and, when it belongs to a session, its paired token)
     * by adding their {@code jti}s to the revocation set. After this call
     * {@link #isRevoked(String)} returns true for those tokens.
     */
    void revoke(String token);

    /**
     * Revoke a single session (adds its token {@code jti}s to the revocation set).
     */
    void revokeBySessionId(String sessionId);

    /**
     * Revoke all sessions of the given user (adds every held token {@code jti}
     * to the revocation set).
     */
    void revokeByUserId(Long userId);

    /**
     * Returns true when the token's {@code jti} is in the revocation set
     * (i.e. it has been revoked). Unknown-but-valid tokens return false.
     */
    boolean isRevoked(String token);
}
