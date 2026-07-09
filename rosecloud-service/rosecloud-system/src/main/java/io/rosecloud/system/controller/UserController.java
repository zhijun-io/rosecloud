package io.rosecloud.system.controller;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.api.user.AuthUserInfo;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.service.UserService;
import io.rosecloud.system.service.dto.ChangePasswordRequest;
import io.rosecloud.system.service.dto.UserCreateRequest;
import io.rosecloud.system.service.dto.UserProfile;
import io.rosecloud.system.service.dto.UserRoleAssignRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDateTime;

/**
 * System user endpoints and Feign-facing auth hooks.
 */
@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/system/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasAuthority('system:user:add')")
    @PostMapping
    public ApiResponse<Long> create(@RequestBody UserCreateRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    @PreAuthorize("hasAuthority('system:user:list')")
    @GetMapping
    public ApiResponse<PageResult<User>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(userService.page(current, size, keyword));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ApiResponse<UserProfile> me() {
        return ApiResponse.ok(userService.me());
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/password")
    public ApiResponse<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:user:list')")
    @GetMapping("/{id}")
    public ApiResponse<User> get(@PathVariable Long id) {
        return ApiResponse.ok(userService.get(id));
    }

    @PreAuthorize("hasAuthority('system:user:del')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:user:list')")
    @GetMapping("/{id}/roles")
    public ApiResponse<List<Long>> roles(@PathVariable Long id) {
        return ApiResponse.ok(userService.findRoleIdsByUserId(id));
    }

    @PreAuthorize("hasAuthority('system:user:edit')")
    @PutMapping("/{id}/roles")
    public ApiResponse<Void> assignRoles(@PathVariable Long id, @RequestBody UserRoleAssignRequest request) {
        userService.assignRoles(id, request.roleIds());
        return ApiResponse.ok();
    }

    @GetMapping("/auth/{username}")
    public ApiResponse<SecurityUser> getAuthInfo(@PathVariable String username) {
        return ApiResponse.ok(userService.loadByUsername(username));
    }
}
