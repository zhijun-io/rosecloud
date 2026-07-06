package io.rosecloud.starter.audit;

/**
 * Resolves the current actor for audit entries. Supply a bean to override the
 * default (which returns {@code "unknown"}), e.g. reading from Spring Security.
 */
public interface AuditPrincipalResolver {

    String resolve();
}
