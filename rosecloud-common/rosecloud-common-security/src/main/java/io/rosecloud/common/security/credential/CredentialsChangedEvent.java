package io.rosecloud.common.security.credential;

/**
 * Pure-JDK record raised when a user's password is changed (created or updated).
 * Inspired by ThingsBoard's {@code UserCredentialsInvalidationEvent}, which carries
 * a {@code userId} and is consumed by the session manager to revoke active sessions.
 *
 * <p>This record carries no Spring dependency so it lives in the no-Spring
 * {@code rosecloud-common-security} module. Services publish it through Spring's
 * {@code ApplicationEventPublisher} and a local {@code @EventListener} handles
 * the actual session revocation (see {@code CredentialsChangedEventListener} in the
 * auth service).
 *
 * @param userId the user whose credentials changed
 */
public record CredentialsChangedEvent(Long userId) {}
