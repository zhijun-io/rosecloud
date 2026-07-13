package io.rosecloud.system.controller;
import lombok.RequiredArgsConstructor;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.system.domain.Dept;
import io.rosecloud.system.service.DeptService;
import io.rosecloud.system.service.dto.DeptRequest;
import io.rosecloud.system.service.dto.DeptTreeNode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/depts")
public class DeptController {

    private final DeptService deptService;
    @PreAuthorize("hasAuthority('system:dept:add')")
    @PostMapping
    public ApiResponse<Long> create(@RequestBody DeptRequest request) {
        return ApiResponse.ok(deptService.create(request));
    }

    @PreAuthorize("hasAuthority('system:dept:edit')")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody DeptRequest request) {
        deptService.update(id, request);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:dept:del')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:dept:list')")
    @GetMapping
    public ApiResponse<List<Dept>> list() {
        return ApiResponse.ok(deptService.list());
    }

    @PreAuthorize("hasAuthority('system:dept:list')")
    @GetMapping("/tree")
    public ApiResponse<List<DeptTreeNode>> tree() {
        return ApiResponse.ok(deptService.tree());
    }
}
