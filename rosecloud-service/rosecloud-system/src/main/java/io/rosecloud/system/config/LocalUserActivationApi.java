package io.rosecloud.system.config;

import io.rosecloud.api.user.ActivationConfirmRequest;
import io.rosecloud.api.user.ActivationResendRequest;
import io.rosecloud.api.user.UserActivationApi;
import io.rosecloud.api.user.UserActivationInfo;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.system.service.UserActivationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * In-process {@link UserActivationApi} for monolith mode: delegates to
 * {@link UserActivationService} instead of crossing process boundaries.
 */
@Component
@ConditionalOnProperty(prefix = "spring.application", name = "name", havingValue = "rosecloud-monolith")
public class LocalUserActivationApi implements UserActivationApi {

    private final UserActivationService userActivationService;

    public LocalUserActivationApi(UserActivationService userActivationService) {
        this.userActivationService = userActivationService;
    }

    @Override
    public ApiResponse<UserActivationInfo> check(String activateToken) {
        return ApiResponse.ok(userActivationService.check(activateToken));
    }

    @Override
    public ApiResponse<UserActivationInfo> confirm(ActivationConfirmRequest request) {
        return ApiResponse.ok(userActivationService.confirm(request.activateToken(), request.password()));
    }

    @Override
    public ApiResponse<UserActivationInfo> resend(ActivationResendRequest request) {
        return ApiResponse.ok(userActivationService.resend(request.username()));
    }
}
