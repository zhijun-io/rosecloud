package io.rosecloud.system.controller;

 import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.core.model.ApiResponse;
import java.time.LocalDateTime;
import io.rosecloud.system.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Internal endpoints consumed by other services over Feign, not routed by the
 * gateway. The auth/security path uses username as the stable lookup key and
 * expects a null payload when no user exists.
 */
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/auth/{username}")
    public ApiResponse<SecurityUser> getAuthInfo(@PathVariable String username) {
        return ApiResponse.ok(userService.loadByUsername(username).orElse(null));
    }

    @PostMapping("/{userId}/last-login")
    public ApiResponse<Void> updateLastLoginTime(@PathVariable Long userId,
                                                 @RequestBody LocalDateTime lastLoginTime) {
        userService.updateLastLoginTime(userId, lastLoginTime);
        return ApiResponse.ok();
    }
}
