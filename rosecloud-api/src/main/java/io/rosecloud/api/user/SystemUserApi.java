package io.rosecloud.api.user;

import io.rosecloud.common.core.model.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Internal Feign contract for auth-relevant user data owned by the system
 * service. Callers use the unique username as the lookup key; JWTs only carry
 * that username plus token metadata, and the full user snapshot is hydrated
 * from this contract after token verification.
 */
@FeignClient(name = "rosecloud-system", contextId = "systemUserApi", path = "/internal/users")
public interface SystemUserApi {

    /** Returns the auth snapshot for a username, or {@code null} data if not found. */
    @GetMapping("/auth/{username}")
    ApiResponse<UserAuthInfo> getAuthInfo(@PathVariable("username") String username);
}
