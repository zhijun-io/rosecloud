package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.PageResult;

/** Repository port for the login audit log. */
public interface LoginLogRepository {

    void insert(LoginLog log);

    PageResult<LoginLog> page(long current, long size, String username, Boolean success);
}
