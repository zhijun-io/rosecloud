package io.rosecloud.system.controller;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.system.support.PageSupport;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.service.AuditLogService;
import io.rosecloud.system.service.TenantService;
import io.rosecloud.system.service.dto.TenantCreateRequest;
import io.rosecloud.system.service.dto.TenantUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/tenants")
public class TenantController {

    private final TenantService tenantService;
    private final AuditLogService auditLogService;

    public TenantController(TenantService tenantService, AuditLogService auditLogService) {
        this.tenantService = tenantService;
        this.auditLogService = auditLogService;
    }

    @PreAuthorize("hasAuthority('system:tenant:add')")
    @PostMapping
    public ApiResponse<String> create(@Valid @RequestBody TenantCreateRequest request) {
        return ApiResponse.ok(tenantService.create(request));
    }

    @PreAuthorize("hasAuthority('system:tenant:edit')")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable String id, @RequestBody TenantUpdateRequest request) {
        tenantService.update(id, request);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:tenant:del')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        tenantService.delete(id);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:tenant:open')")
    @PostMapping("/{id}/open")
    public ApiResponse<String> open(@PathVariable String id) {
        return ApiResponse.ok(tenantService.open(id));
    }

    @PreAuthorize("hasAuthority('system:tenant:list')")
    @GetMapping("/{id}")
    public ApiResponse<Tenant> get(@PathVariable String id) {
        return ApiResponse.ok(tenantService.get(id));
    }

    @PreAuthorize("hasAuthority('system:tenant:toggle')")
    @PostMapping("/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable String id) {
        tenantService.disable(id);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:tenant:toggle')")
    @PostMapping("/{id}/enable")
    public ApiResponse<Void> enable(@PathVariable String id) {
        tenantService.enable(id);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:tenant:list')")
    @GetMapping
    public ApiResponse<PageResult<Tenant>> page(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(tenantService.page(PageSupport.current(current), PageSupport.size(size), keyword));
    }
}
