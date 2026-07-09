package io.rosecloud.api.user;

import io.rosecloud.common.security.model.SecurityUser;

/**
 * Service contract for auth-relevant user data owned by the system service.
 * Transport-specific annotations live on {@link UserFeignApi}.
 */
public interface UserApi {

    SecurityUser loadUserByUsername(String username);
}
