package io.rosecloud.starter.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

/**
 * Logs {@link AuditLogEvent}s via a dedicated logger so audited operations are
 * observable without a persistence backend. Supplement with a listener that
 * persists to the audit store (e.g. in {@code rosecloud-system}).
 */
public class AuditLogEventListener {

    private static final Logger log = LoggerFactory.getLogger("io.rosecloud.audit");

    @EventListener
    public void onAuditLogEvent(AuditLogEvent event) {
        if (event.failure() == null) {
            log.info("audit action={} principal={} tenant={} target={} elapsed={}ms desc={}",
                    event.action(), event.principal(), event.tenantId(), event.target(),
                    event.elapsedMillis(), event.description());
        } else {
            log.warn("audit action={} principal={} tenant={} target={} elapsed={}ms failure={}",
                    event.action(), event.principal(), event.tenantId(), event.target(),
                    event.elapsedMillis(), event.failure().toString());
        }
    }
}
