package io.rosecloud.system.service;

import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.LoginLog;

public interface LoginLogService {

    ApiResponse<Void> record(LoginLogRequest request);

    PageResult<LoginLog> page(long current, long size, String username, Boolean success);
}
