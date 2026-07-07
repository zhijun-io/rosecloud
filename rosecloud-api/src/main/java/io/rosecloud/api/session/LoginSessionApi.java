package io.rosecloud.api.session;

import io.rosecloud.common.core.model.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign contract for login-session tracking in the system service. The auth
 * service records a session on login (by token {@code jti}) and marks it
 * logged out on logout, so the system can list online users / sessions.
 */
@FeignClient(name = "rosecloud-system", contextId = "loginSessionApi", path = "/internal/sessions")
public interface LoginSessionApi {

    @PostMapping
    ApiResponse<Void> record(@RequestBody LoginSessionRequest request);

    @PostMapping("/logout-by-jti")
    ApiResponse<Void> logoutByJti(@RequestParam("jti") String jti);
}
