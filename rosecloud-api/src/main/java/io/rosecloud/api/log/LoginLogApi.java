package io.rosecloud.api.log;

import io.rosecloud.common.core.model.ApiResponse;

/**
 * Internal contract for recording login attempts in the system service's login
 * audit log. Transport-specific annotations live on {@link LoginLogFeignApi}.
 */
public interface LoginLogApi {

    ApiResponse<Void> record(LoginLogRequest request);
}
