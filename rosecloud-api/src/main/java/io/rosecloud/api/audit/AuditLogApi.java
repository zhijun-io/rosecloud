package io.rosecloud.api.audit;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

/**
 * Feign interface for remote audit log persistence. Called by the async listener
 * in services that have no local {@code AuditLogRepository} (e.g. notice in microservices
 * mode). The system service provides the REST endpoint at {@code /api/system/audit-logs}.
 */
public interface AuditLogApi {

    void save(AuditLogRequest auditLogRequest);
}
