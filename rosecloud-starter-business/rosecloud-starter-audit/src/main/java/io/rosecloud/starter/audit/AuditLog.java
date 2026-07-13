package io.rosecloud.starter.audit;

import io.rosecloud.api.audit.AuditLogRequest;

import java.lang.annotation.*;

/**
 * Marks a method for audit logging. When {@code rosecloud.audit.enabled=true},
 * the surrounding aspect publishes an {@link AuditLogRequest} on completion
 * (success or failure).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /** Action label, e.g. {@code "login"}. Defaults to the method name. */
    String action() default "";

    /** Free-form description of the audited operation. */
    String description() default "";

    /** Entity type being acted on, e.g. {@code "user"}, {@code "role"}, {@code "tenant"}. Empty if not applicable. */
    String entityType() default "";

    /**
     * SpEL expression resolved against the annotated method's arguments to extract the
     * affected entity id, e.g. {@code "#req.id"} or {@code "#id"}. Evaluated only when
     * {@link #entityType()} is non-blank; empty if not applicable.
     */
    String entityId() default "";

    /**
     * Severity label, e.g. {@code "INFO"}, {@code "WARN"}, {@code "CRITICAL"}.
     * Empty falls back to {@code "INFO"} on success or {@code "ERROR"} on failure.
     */
    String severity() default "";
}
