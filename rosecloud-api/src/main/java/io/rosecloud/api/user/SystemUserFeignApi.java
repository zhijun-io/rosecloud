package io.rosecloud.api.user;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.model.SecurityUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "rosecloud-system", contextId = "systemUserApi", path = "/internal/users")
public interface SystemUserFeignApi extends SystemUserApi {

    @Override
    @GetMapping("/auth/{username}")
    ApiResponse<SecurityUser> loadUserByUsername(@PathVariable("username") String username);

    @Override
    @PostMapping("/{userId}/last-login")
    ApiResponse<Void> updateLastLoginTime(@PathVariable("userId") Long userId,
                                          @RequestBody java.time.LocalDateTime lastLoginTime);

    @Override
    @PostMapping("/{userId}/password")
    ApiResponse<Void> updatePassword(@PathVariable("userId") Long userId,
                                     @RequestBody UserPasswordUpdateRequest request);
}
