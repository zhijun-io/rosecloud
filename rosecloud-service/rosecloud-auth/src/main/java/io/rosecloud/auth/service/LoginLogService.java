package io.rosecloud.auth.service;

import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.TimePageQuery;
import io.rosecloud.auth.domain.LoginLog;

public interface LoginLogService {

    void record(LoginLogRequest request);

    PagedData<LoginLog> page(TimePageQuery pageQuery, String username, Boolean success);
}
