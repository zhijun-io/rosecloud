package io.rosecloud.system.config;

import io.rosecloud.api.user.SystemUserApi;
import io.rosecloud.api.user.UserAuthInfo;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.system.service.UserService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * In-process {@link SystemUserApi} for the system service and monolith mode:
 * delegates to {@link UserService} instead of going through Feign.
 */
@Primary
@Component
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
