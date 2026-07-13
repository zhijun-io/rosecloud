package io.rosecloud.system.controller;
import lombok.RequiredArgsConstructor;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.system.domain.TenantProfile;
import io.rosecloud.system.service.TenantProfileService;
import io.rosecloud.system.service.dto.TenantProfileCreateRequest;
import io.rosecloud.system.service.dto.TenantProfileUpdateRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/tenant-profiles")
public class TenantProfileController {

    private final TenantProfileService tenantProfileService;
    @PreAuthorize("hasAuthority('system:tenant-profile:add')")
    @PostMapping
    public ApiResponse<String> create(@RequestBody TenantProfileCreateRequest request) {
        return ApiResponse.ok(tenantProfileService.create(request));
    }

    @PreAuthorize("hasAuthority('system:tenant-profile:edit')")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable String id, @RequestBody TenantProfileUpdateRequest request) {
        tenantProfileService.update(id, request);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:tenant-profile:del')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        tenantProfileService.delete(id);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:tenant-profile:default')")
    @PutMapping("/{id}/default")
    public ApiResponse<Void> makeDefault(@PathVariable String id) {
        tenantProfileService.makeDefault(id);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:tenant-profile:list')")
    @GetMapping
    public ApiResponse<List<TenantProfile>> list() {
        return ApiResponse.ok(tenantProfileService.list());
    }

    @PreAuthorize("hasAuthority('system:tenant-profile:list')")
    @GetMapping("/default")
    public ApiResponse<TenantProfile> getDefault() {
        return ApiResponse.ok(tenantProfileService.getDefault());
    }

    @PreAuthorize("hasAuthority('system:tenant-profile:list')")
    @GetMapping("/{id}")
    public ApiResponse<TenantProfile> get(@PathVariable String id) {
        return ApiResponse.ok(tenantProfileService.get(id));
    }
}
