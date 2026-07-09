package io.rosecloud.api.user;

import io.rosecloud.common.core.model.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "rosecloud-system", contextId = "systemUserApi", path = "/api/system/users")
public interface SystemUserFeignApi extends SystemUserApi {

    @Override
    @GetMapping("/auth/{username}")
    ApiResponse<AuthUserInfo> loadUserByUsername(@PathVariable("username") String username);

}
