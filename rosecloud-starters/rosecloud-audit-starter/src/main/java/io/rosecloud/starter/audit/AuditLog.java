package io.rosecloud.starter.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for audit logging. When {@code rosecloud.audit.enabled=true},
 * the surrounding aspect publishes an {@link AuditLogEvent} on completion
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
