package io.rosecloud.system.controller;
import lombok.RequiredArgsConstructor;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.starter.security.annotation.InternalApi;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.service.UserService;
import io.rosecloud.system.service.dto.ChangePasswordRequest;
import io.rosecloud.system.service.dto.UserCreateRequest;
import io.rosecloud.system.service.dto.UserProfile;
import io.rosecloud.system.service.dto.UserRoleAssignRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * System user endpoints and Feign-facing auth hooks.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/users")
public class UserController {

    private final UserService userService;
    @PreAuthorize("hasAuthority('system:user:add')")
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    @PreAuthorize("hasAuthority('system:user:list')")
    @GetMapping
    public ApiResponse<PagedData<User>> page(PageQuery pageQuery) {
        return ApiResponse.ok(userService.page(pageQuery));
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

    @InternalApi
    @GetMapping("/auth/{username}")
    public ApiResponse<SecurityUser> loadByUsername(@PathVariable String username) {
        return ApiResponse.ok(userService.loadByUsername(username));
    }
}
