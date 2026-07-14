package io.rosecloud.auth.service.impl;

import io.rosecloud.auth.persistence.LoginSessionDao;
import io.rosecloud.auth.persistence.LoginSessionEntity;
import io.rosecloud.auth.service.LoginSessionService;
import io.rosecloud.common.security.model.LoginSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * DB-backed {@link LoginSessionService}. The {@code auth_login_session} table is the
 * authoritative (and only) store for sessions and token revocation — Redis is no longer
 * used. Revocation is expressed by the {@code revoked} column, so {@link #isRevoked(String)}
 * is a single indexed lookup; {@code token}/{@code refresh_token} carry prefix indexes so the
 * per-request check stays index-backed.
 */
@Service
@RequiredArgsConstructor
public class LoginSessionServiceImpl implements LoginSessionService {

    private final LoginSessionDao sessionDao;

    @Value("${rosecloud.auth.max-concurrent-sessions:5}")
    private int maxConcurrentSessions;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(LoginSession session) {
        enforceConcurrentLimit(session.userId());
        sessionDao.insert(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(String token) {
        sessionDao.markRevokedByToken(token);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeBySessionId(String sessionId) {
        sessionDao.markRevokedBySessionId(sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeByUserId(Long userId) {
        sessionDao.markRevokedByUserId(userId);
    }

    @Override
    public boolean isRevoked(String token) {
        return sessionDao.countActiveByToken(token) > 0;
    }

    @Override
    public Optional<LoginSession> findBySessionId(String sessionId) {
        return sessionDao.findBySessionId(sessionId);
    }

    @Override
    public Optional<LoginSession> findByToken(String token) {
        return sessionDao.findByToken(token);
    }

    @Override
    public List<LoginSession> findByUserId(Long userId) {
        return sessionDao.findByUserId(userId);
    }

    @Override
    public List<LoginSession> findAll() {
        return sessionDao.findAllActive();
    }

    private void enforceConcurrentLimit(Long userId) {
        if (maxConcurrentSessions <= 0) {
            return;
        }
        List<LoginSessionEntity> active = sessionDao.findActiveEntitiesByUserId(userId);
        int excess = active.size() - maxConcurrentSessions + 1;
        for (int i = 0; i < excess; i++) {
            sessionDao.markRevokedBySessionId(active.get(i).getSessionId());
        }
    }
}
