package io.rosecloud.system.controller;

import io.rosecloud.api.audit.AuditLogApi;
import io.rosecloud.api.audit.AuditLogRequest;
import io.rosecloud.system.service.AuditLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST endpoint for remote audit log persistence (microservices mode).
 * Called by {@link AuditLogApi} (Feign) from
 * services that have no local audit log repository (e.g. notice).
 *
 * <p>In monolith mode, {@link AuditLogService} is used directly via
 * {@code AuditLogPersistenceListener} in the same Spring context; this endpoint
 * is not called.
 */
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
