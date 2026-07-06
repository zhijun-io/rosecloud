package io.rosecloud.system.config;

import io.rosecloud.api.session.LoginSessionApi;
import io.rosecloud.api.session.LoginSessionRequest;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.system.service.LoginSessionService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * In-process {@link LoginSessionApi} for monolith mode: delegates to
 * {@link LoginSessionService} instead of Feign. Only active under the
 * {@code monolith} profile.
 */
@Profile("monolith")
@Component
public class LocalLoginSessionApi implements LoginSessionApi {

    private final LoginSessionService loginSessionService;

    public LocalLoginSessionApi(LoginSessionService loginSessionService) {
        this.loginSessionService = loginSessionService;
    }

    @Override
    public ApiResponse<Void> record(LoginSessionRequest request) {
        loginSessionService.record(request);
        return ApiResponse.ok();
    }

    @Override
    public ApiResponse<Void> logoutByJti(String jti) {
        loginSessionService.logoutByJti(jti);
        return ApiResponse.ok();
    }
}
