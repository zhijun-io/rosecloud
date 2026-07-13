package io.rosecloud.api.audit;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Feign interface for remote audit log persistence. Called by the async listener
 * in services that have no local {@code AuditLogRepository} (e.g. notice in microservices
 * mode). The system service provides the REST endpoint at {@code /api/audit-logs}.
 */
@FeignClient(name = "rosecloud-system", contextId = "auditLogApi", path = "/api/audit-logs")
public interface AuditLogFeignApi extends AuditLogApi{

    @PostMapping
    void save(AuditLogRequest auditLogRequest);
}
