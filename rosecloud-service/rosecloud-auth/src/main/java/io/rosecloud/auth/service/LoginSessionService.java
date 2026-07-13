package io.rosecloud.auth.service;

import io.rosecloud.starter.security.session.LoginSessionApi;
import io.rosecloud.common.security.model.LoginSession;

import java.util.List;
import java.util.Optional;

/**
 * Auth-owned session service. Extends the cross-service {@link LoginSessionApi} contract and
 * adds admin query methods. The implementation ({@code LoginSessionServiceImpl}) is database-backed
 * only (no Redis) via {@code LoginSessionMapper}.
 */
public interface LoginSessionService extends io.rosecloud.starter.security.session.LoginSessionApi {

    Optional<LoginSession> findBySessionId(String sessionId);

    Optional<LoginSession> findByToken(String token);

    List<LoginSession> findByUserId(Long userId);

    List<LoginSession> findAll();
}
