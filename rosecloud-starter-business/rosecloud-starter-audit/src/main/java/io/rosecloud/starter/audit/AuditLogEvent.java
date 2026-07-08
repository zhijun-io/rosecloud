package io.rosecloud.starter.audit;

import java.time.Instant;

/**
 * Event published when an {@link AuditLog}-annotated method completes (success
 * or failure). Carries the operator, tenant, target method and timing so a
 * listener can persist or forward it.
 */
public record AuditLogEvent(
        String action,
        String description,
        String principal,
        String tenantId,
        String target,
        long elapsedMillis,
        Instant timestamp,
        Throwable failure
) {
}
