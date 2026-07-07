package io.rosecloud.system.audit;

import io.rosecloud.starter.audit.AuditLogEvent;
import io.rosecloud.system.domain.AuditLog;
import io.rosecloud.system.domain.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Persists {@link AuditLogEvent}s to {@code sys_audit_log}. In microservice mode
 * it captures this service's own audited operations; in the monolith, every
 * service's events flow through the single context. Persistence failures are
 * logged but never propagate, so auditing cannot break the audited operation.
 */
@Component
public class AuditLogPersistenceListener {

    private static final Logger log = LoggerFactory.getLogger(AuditLogPersistenceListener.class);

    private final AuditLogRepository repository;

    public AuditLogPersistenceListener(AuditLogRepository repository) {
        this.repository = repository;
    }

    @EventListener
    public void onAuditLogEvent(AuditLogEvent event) {
        try {
            repository.insert(new AuditLog(null, event.action(), event.description(), event.principal(),
                    event.tenantId(), event.target(), event.elapsedMillis(), event.failure() == null,
                    event.failure() == null ? null : event.failure().toString(), null));
        } catch (Exception e) {
            log.warn("failed to persist audit log: action={} target={}", event.action(), event.target(), e);
        }
    }
}
