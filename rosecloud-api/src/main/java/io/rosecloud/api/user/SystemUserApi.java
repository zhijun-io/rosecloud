package io.rosecloud.api.user;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.model.SecurityUser;

/**
 * Internal user contract for auth-relevant user data owned by the system
 * service. Transport-specific annotations live on {@link SystemUserFeignApi}.
 */
public interface SystemUserApi {

    ApiResponse<SecurityUser> loadUserByUsername(String username);

    ApiResponse<Void> updateLastLoginTime(Long userId, java.time.LocalDateTime lastLoginTime);

    ApiResponse<Void> updatePassword(Long userId, UserPasswordUpdateRequest request);
}
