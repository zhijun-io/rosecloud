package io.rosecloud.system.controller;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.system.domain.AuditLog;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.service.AuditLogService;
import io.rosecloud.system.service.TenantService;
import io.rosecloud.system.service.dto.TenantApplyRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/system/tenants")
public class TenantController {

    private final TenantService tenantService;
    private final AuditLogService auditLogService;

    public TenantController(TenantService tenantService, AuditLogService auditLogService) {
        this.tenantService = tenantService;
        this.auditLogService = auditLogService;
    }

    @PreAuthorize("hasAuthority('system:tenant:open')")
    @PostMapping("/apply")
    public ApiResponse<Long> apply(@RequestBody TenantApplyRequest request) {
        return ApiResponse.ok(tenantService.apply(request));
    }

    @PreAuthorize("hasAuthority('system:tenant:open')")
    @PostMapping("/{id}/open")
    public ApiResponse<Long> open(@PathVariable Long id) {
        return ApiResponse.ok(tenantService.open(id));
    }

    @PreAuthorize("hasAuthority('system:tenant:list')")
    @GetMapping("/{id}")
    public ApiResponse<Tenant> get(@PathVariable Long id) {
        return ApiResponse.ok(tenantService.get(id));
    }

    @PreAuthorize("hasAuthority('system:tenant:toggle')")
    @PostMapping("/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        tenantService.disable(id);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:tenant:toggle')")
    @PostMapping("/{id}/enable")
    public ApiResponse<Void> enable(@PathVariable Long id) {
        tenantService.enable(id);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('system:tenant:list')")
    @GetMapping
    public ApiResponse<PageResult<Tenant>> page(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(tenantService.page(current, size, keyword));
    }

    @PreAuthorize("hasAuthority('system:audit:list')")
    @GetMapping("/{id}/audit")
    public ApiResponse<PageResult<AuditLog>> audit(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "1") long current,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) String action,
                                                   @RequestParam(required = false) String principal) {
        return ApiResponse.ok(auditLogService.page(current, size, id, action, principal));
    }
}
