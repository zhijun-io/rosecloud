package io.rosecloud.system.config;

import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.system.service.LoginLogService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * In-process {@link LoginLogApi} for monolith mode: delegates to
 * {@link LoginLogService} instead of Feign. Only active under the
 * {@code monolith} profile.
 */
@Profile("monolith")
@Component
public class LocalLoginLogApi implements LoginLogApi {

    private final LoginLogService loginLogService;

    public LocalLoginLogApi(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    @Override
    public ApiResponse<Void> record(LoginLogRequest request) {
        loginLogService.record(request);
        return ApiResponse.ok();
    }
}
