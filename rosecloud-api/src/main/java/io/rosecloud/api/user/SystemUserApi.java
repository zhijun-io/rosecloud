package io.rosecloud.api.user;

import io.rosecloud.common.core.model.ApiResponse;
 import io.rosecloud.common.security.model.SecurityUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Internal Feign contract for auth-relevant user data owned by the system
 * service. Callers use the unique login identifier as the lookup key; JWTs only
 * carry that identifier plus token metadata, and the full user snapshot is
 * hydrated from this contract after token verification.
 */
@FeignClient(name = "rosecloud-system", contextId = "systemUserApi", path = "/internal/users")
public interface SystemUserApi {

    /** Returns the auth snapshot for a login identifier, or {@code null} data if not found. */
        @GetMapping("/auth/{username}")
    ApiResponse<SecurityUser> getAuthInfo(@PathVariable("username") String username);

    /** Records the timestamp of the user's last successful login. */
    @PostMapping("/{userId}/last-login")
    ApiResponse<Void> updateLastLoginTime(@PathVariable("userId") Long userId,
                                          @RequestBody java.time.LocalDateTime lastLoginTime);

    /** Updates the user's password (current password verification is performed server-side). */
    @PostMapping("/{userId}/password")
    ApiResponse<Void> updatePassword(@PathVariable("userId") Long userId,
                                     @RequestBody UserPasswordUpdateRequest request);
}
