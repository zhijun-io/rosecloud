package io.rosecloud.system.controller;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.system.domain.Role;
import io.rosecloud.system.service.RoleService;
import io.rosecloud.system.service.dto.RoleCreateRequest;
import io.rosecloud.system.service.dto.RoleMenuAssignRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/system/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody RoleCreateRequest request) {
        return ApiResponse.ok(roleService.create(request));
    }

    @GetMapping("/{id}/menus")
    public ApiResponse<List<Long>> menus(@PathVariable Long id) {
        return ApiResponse.ok(roleService.findMenuIdsByRoleId(id));
    }

    @PutMapping("/{id}/menus")
    public ApiResponse<Void> assignMenus(@PathVariable Long id, @RequestBody RoleMenuAssignRequest request) {
        roleService.assignMenus(id, request.menuIds());
        return ApiResponse.ok();
    }

    @GetMapping
    public ApiResponse<PageResult<Role>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(roleService.page(current, size, keyword));
    }
}
