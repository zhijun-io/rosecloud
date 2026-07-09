package io.rosecloud.common.security.session;

import io.rosecloud.common.security.model.LoginSession;


/**
 * Unified store for login sessions and token revocation state.
 * Serves both the authentication chain (isRevoked checks on every request)
 * and session administration (save on login, revoke on logout/kick).
 *
 * <p>Revocation granularity:
 * <ul>
 *   <li>{@link #revoke(String)} — revoke ALL sessions issued under a token
 *   <li>{@link #revokeBySessionId(String)} — revoke a single session
 *   <li>{@link #revokeByUserId(Long)} — revoke ALL sessions of a user
 * </ul>
 *
 * Implementations must be fast on isRevoked() — it is called on every request.
 */
public interface SessionStore {

    void save(LoginSession session);

    /**
     * Revoke all sessions for the given token. After this call
     * {@link #isRevoked(String)} returns true for the token.
     */
    void revoke(String token);

    /**
     * Revoke a single session. Other sessions for the same token remain valid.
     */
    void revokeBySessionId(String sessionId);

    /**
     * Revoke all sessions of the given user. Every token the user holds
     * will be considered revoked.
     */
    void revokeByUserId(Long userId);

    /**
     * Returns true when no active session exists for the token
     * (i.e. it has been revoked by any of the revoke methods).
     */
    boolean isRevoked(String token);
}
