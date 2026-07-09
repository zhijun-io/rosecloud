package io.rosecloud.system.controller;

import io.rosecloud.api.audit.AuditLogRequest;
import io.rosecloud.system.service.AuditLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:auditlog:save')")
    public void save(@RequestBody AuditLogRequest auditLogRequest) {
        auditLogService.save(auditLogRequest);
    }
}
