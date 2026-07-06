package io.rosecloud.starter.audit;

/**
 * Resolves the current actor for audit entries. Supply a bean to override the
 * default (which reads {@code UserContext}), e.g. a custom Spring Security source.
 */
public interface AuditPrincipalResolver {

    String resolve();
}
