package io.rosecloud.system.controller;

import io.rosecloud.api.user.UserAuthInfo;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.system.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal endpoints consumed by other services over Feign (not routed by the
 * gateway). Reachable only via direct service-to-service calls; returns
 * {@code null} data when a user is not found.
 */
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/auth/{username}")
    public ApiResponse<UserAuthInfo> getAuthInfo(@PathVariable String username) {
        return ApiResponse.ok(userService.findAuthInfo(username).orElse(null));
    }
}
