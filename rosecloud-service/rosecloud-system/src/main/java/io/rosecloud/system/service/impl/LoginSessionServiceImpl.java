package io.rosecloud.system.service.impl;

import io.rosecloud.api.session.RevokeRequest;
import io.rosecloud.api.session.TokenRevocationApi;
import io.rosecloud.api.session.LoginSessionRequest;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.LoginSession;
import io.rosecloud.system.domain.LoginSessionRepository;
import io.rosecloud.system.domain.LoginSessionStatus;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.LoginSessionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoginSessionServiceImpl implements LoginSessionService {

    private final LoginSessionRepository loginSessionRepository;
    private final TokenRevocationApi tokenRevocationApi;

    public LoginSessionServiceImpl(LoginSessionRepository loginSessionRepository,
                                   TokenRevocationApi tokenRevocationApi) {
        this.loginSessionRepository = loginSessionRepository;
        this.tokenRevocationApi = tokenRevocationApi;
    }

    @Override
    public void record(LoginSessionRequest request) {
        LocalDateTime now = LocalDateTime.now();
        LoginSession session = new LoginSession(null, request.jti(), request.userId(), request.username(),
                request.tenantId(), now, request.expireTime(), request.ip(), request.userAgent(),
                LoginSessionStatus.ONLINE.code());
        loginSessionRepository.insert(session);
    }

    @Override
    public void logoutByJti(String jti) {
        loginSessionRepository.markLoggedOutByJti(jti);
    }

    @Override
    public PageResult<LoginSession> onlinePage(long current, long size) {
        return loginSessionRepository.onlinePage(current, size, scopeTenantId(), LocalDateTime.now());
    }

    @AuditLog(action = "session-kick", description = "强制下线")
    @Override
    public void kick(Long id) {
        LoginSession session = loginSessionRepository.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.SESSION_NOT_FOUND));
        loginSessionRepository.markLoggedOutById(id);
        if (session.getJti() != null) {
            tokenRevocationApi.revoke(new RevokeRequest(session.getJti(), session.getExpireTime()));
        }
    }

    /** Returns the caller's tenant id, or null for platform admins (who see all sessions). */
    private static Long scopeTenantId() {
        CurrentUser user = UserContext.get();
        return user == null ? null : user.tenantId();
    }
}
