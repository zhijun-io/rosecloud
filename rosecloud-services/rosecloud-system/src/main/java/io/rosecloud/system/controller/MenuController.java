package io.rosecloud.system.controller;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.system.domain.Menu;
import io.rosecloud.system.service.MenuService;
import io.rosecloud.system.service.dto.MenuRequest;
import io.rosecloud.system.service.dto.MenuTreeNode;
import io.rosecloud.system.service.dto.UserMenuResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/system/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody MenuRequest request) {
        return ApiResponse.ok(menuService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody MenuRequest request) {
        menuService.update(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping
    public ApiResponse<List<Menu>> list() {
        return ApiResponse.ok(menuService.list());
    }

    @GetMapping("/tree")
    public ApiResponse<List<MenuTreeNode>> tree() {
        return ApiResponse.ok(menuService.tree());
    }

    @GetMapping("/me")
    public ApiResponse<UserMenuResult> myMenus() {
        return ApiResponse.ok(menuService.myMenus());
    }
}
