package io.rosecloud.api.user;

import io.rosecloud.common.core.model.ApiResponse;

/**
 * Service contract for auth-relevant user data owned by the system service.
 * Transport-specific annotations live on {@link SystemUserFeignApi}.
 */
public interface SystemUserApi {

    ApiResponse<AuthUserInfo> loadUserByUsername(String username);
}
