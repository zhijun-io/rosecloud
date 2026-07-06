package io.rosecloud.starter.audit;

import java.time.Instant;

/** Event published when an {@link AuditLog}-annotated method completes. */
public record AuditLogEvent(
        String action,
        String description,
        String principal,
        long elapsedMillis,
        Instant timestamp,
        Throwable failure
) {
}
