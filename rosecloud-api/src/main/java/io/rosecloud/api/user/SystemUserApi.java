package io.rosecloud.api.user;

import io.rosecloud.common.core.model.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign contract for auth-relevant user data owned by the system service. The
 * auth service calls this instead of touching the user store directly, so the
 * canonical user store lives in a single service.
 */
@FeignClient(name = "rosecloud-system", contextId = "systemUserApi", path = "/internal/users")
public interface SystemUserApi {

    /** Returns the auth snapshot for a username, or {@code null} data if not found. */
    @GetMapping("/auth/{username}")
    ApiResponse<UserAuthInfo> getAuthInfo(@PathVariable("username") String username);
}
