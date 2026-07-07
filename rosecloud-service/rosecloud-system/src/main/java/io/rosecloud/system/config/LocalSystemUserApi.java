package io.rosecloud.system.config;

import io.rosecloud.api.user.SystemUserApi;
import io.rosecloud.api.user.UserAuthInfo;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.system.service.UserService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * In-process {@link SystemUserApi} for monolith mode: delegates to
 * {@link UserService} instead of going through Feign, since the system service
 * runs in the same process. Only active under the {@code monolith} profile.
 */
@Profile("monolith")
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
