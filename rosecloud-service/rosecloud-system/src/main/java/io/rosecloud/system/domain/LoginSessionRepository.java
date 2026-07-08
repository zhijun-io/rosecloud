package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.PageResult;

import java.util.Optional;

/** Repository port for login sessions. */
public interface LoginSessionRepository {

    Long insert(LoginSession session);

    Optional<LoginSession> findById(Long id);

    Optional<LoginSession> findByJti(String jti);

    void markLoggedOutByJti(String jti);

    void markLoggedOutById(Long id);

    /** Active (online, not expired) sessions; scoped to {@code tenantId} when non-null. */
    PageResult<LoginSession> onlinePage(long current, long size, String tenantId, java.time.LocalDateTime now);
}
