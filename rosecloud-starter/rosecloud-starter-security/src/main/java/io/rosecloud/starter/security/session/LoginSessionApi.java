package io.rosecloud.starter.security.session;

import io.rosecloud.common.security.model.LoginSession;

/**
 * Session/revocation abstraction owned by the auth service. The starter declares this
 * interface; the auth service provides the database-backed implementation
 * ({@code LoginSessionServiceImpl} via {@code LoginSessionMapper}), and other services
 * consume the Feign client ({@code LoginSessionFeignApi}). Revocation is therefore
 * enforced centrally in auth (the single authoritative session owner per the IAM boundary)
 * without the starter depending on {@code rosecloud-api} or on the auth service.
 */
public interface LoginSessionApi {

    void save(LoginSession session);

    boolean isRevoked(String token);

    void revoke(String token);

    void revokeBySessionId(String sessionId);

    void revokeByUserId(Long userId);
}
