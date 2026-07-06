package io.rosecloud.system.service;

import io.rosecloud.api.session.LoginSessionRequest;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.LoginSession;

public interface LoginSessionService {

    void record(LoginSessionRequest request);

    void logoutByJti(String jti);

    PageResult<LoginSession> onlinePage(long current, long size);
}
