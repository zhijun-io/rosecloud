package io.rosecloud.system.service;

import io.rosecloud.common.security.model.LoginSession;
import io.rosecloud.common.security.session.SessionStore;
import io.rosecloud.system.service.impl.LoginSessionServiceImpl;

import java.util.List;
import java.util.Optional;

/**
 * Extended {@link SessionStore} with admin query methods.
 * Implemented by {@link LoginSessionServiceImpl}.
 */
public interface LoginSessionService extends SessionStore {

    Optional<LoginSession> findBySessionId(String sessionId);

    Optional<LoginSession> findByToken(String token);

    List<LoginSession> findByUserId(Long userId);

    List<LoginSession> findAll();
}
