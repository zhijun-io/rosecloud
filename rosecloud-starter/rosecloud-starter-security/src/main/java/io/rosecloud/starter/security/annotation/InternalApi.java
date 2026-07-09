package io.rosecloud.starter.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller endpoint as internal: only callable by other services of the
 * platform, never by external clients. Enforced by {@code InternalApiAuthenticationFilter}
 * (which grants {@code ROLE_INTERNAL} when the gateway-trusted {@code X-Internal} header is
 * present) combined with the method-level {@code @PreAuthorize} below.
 *
 * <p>Internal endpoints must stay in {@code rosecloud.security.public-paths} so the JWT filter
 * skips them; this annotation then provides the real access check.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAuthority('ROLE_INTERNAL')")
@Documented
public @interface InternalApi {
}
