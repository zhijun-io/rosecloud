package io.rosecloud.system.controller;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.system.domain.AuditLog;
import io.rosecloud.system.service.AuditLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/system/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @PreAuthorize("hasAuthority('system:audit:list')")
    @GetMapping("/{id}")
    public ApiResponse<AuditLog> get(@PathVariable Long id) {
        return ApiResponse.ok(auditLogService.get(id));
    }

    @PreAuthorize("hasAuthority('system:audit:list')")
    @GetMapping
    public ApiResponse<PageResult<AuditLog>> page(@RequestParam(defaultValue = "1") long current,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) Long tenantId,
                                                   @RequestParam(required = false) String action,
                                                   @RequestParam(required = false) String principal) {
        return ApiResponse.ok(auditLogService.page(current, size, tenantId, action, principal));
    }
}
