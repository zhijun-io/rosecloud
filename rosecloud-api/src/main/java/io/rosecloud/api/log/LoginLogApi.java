package io.rosecloud.api.log;

import io.rosecloud.common.core.model.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign contract for recording login attempts in the system service's login
 * audit log. The auth service (which holds no database) reports each attempt.
 */
@FeignClient(name = "rosecloud-system", path = "/internal/login-logs")
public interface LoginLogApi {

    @PostMapping
    ApiResponse<Void> record(@RequestBody LoginLogRequest request);
}
