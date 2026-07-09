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
}
