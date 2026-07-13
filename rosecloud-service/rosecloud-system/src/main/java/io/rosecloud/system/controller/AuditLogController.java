package io.rosecloud.system.controller;
import lombok.RequiredArgsConstructor;

import io.rosecloud.api.audit.AuditLogRequest;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.common.core.model.TimePageQuery;
import io.rosecloud.system.domain.AuditLog;
import io.rosecloud.system.domain.AuditLogQuery;
import io.rosecloud.system.service.AuditLogService;
import io.rosecloud.starter.security.annotation.InternalApi;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;
    @PreAuthorize("hasAuthority('system:auditlog:list')")
    @GetMapping("/{id}")
    public ApiResponse<AuditLog> get(@PathVariable Long id) {
        return ApiResponse.ok(auditLogService.get(id));
    }

    @PreAuthorize("hasAuthority('system:auditlog:list')")
    @GetMapping
    public ApiResponse<PagedData<AuditLog>> page(TimePageQuery pageQuery,
                                                 @RequestParam(required = false) String action,
                                                 @RequestParam(required = false) String username,
                                                 @RequestParam(required = false) String tenantId,
                                                 @RequestParam(required = false) Boolean success,
                                                 @RequestParam(required = false) String entityType) {
        AuditLogQuery query = AuditLogQuery.of(action, username, tenantId, success, entityType);
        return ApiResponse.ok(auditLogService.page(pageQuery, query));
    }

    @InternalApi
    @PostMapping
    public ApiResponse<Void> save(@RequestBody AuditLogRequest auditLogRequest) {
        auditLogService.save(auditLogRequest);
        return ApiResponse.ok();
    }
}
