package io.rosecloud.system.config;

import io.rosecloud.api.user.SystemUserApi;
import io.rosecloud.api.user.UserAuthInfo;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.system.service.UserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * In-process {@link SystemUserApi} for the system service and monolith mode.
 * It keeps the same contract shape as the Feign client but resolves users
 * locally instead of crossing process boundaries.
 */
@Component
@ConditionalOnProperty(prefix = "spring.application", name = "name", havingValue = "rosecloud-monolith")
public class LocalSystemUserApi implements SystemUserApi {

    private final UserService userService;

    public LocalSystemUserApi(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ApiResponse<UserAuthInfo> getAuthInfo(String username) {
        return ApiResponse.ok(userService.findAuthInfo(username).orElse(null));
    }
}
