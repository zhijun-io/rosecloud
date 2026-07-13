package io.rosecloud.auth.service;

import io.rosecloud.common.security.credential.CredentialsChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for {@link CredentialsChangedEvent} and revokes all active sessions for
 * the affected user. This decouples credential mutation from session management:
 * any service or future code path that changes a password only needs to publish the
 * event, and revocation follows automatically.
 *
 * <p>Inspired by ThingsBoard's {@code UserCredentialsInvalidationEvent} + consumer
 * pattern, though ThingsBoard broadcasts the event cluster-wide while RoseCloud's
 * single-auth-service topology handles it locally and synchronously.
 *
 * <p>The listener runs in the same transaction as the publisher by default, so
 * a failure in session revocation rolls back the credential change.
 */
@RequiredArgsConstructor
@Component
public class CredentialsChangedEventListener {

    private final LoginSessionService loginSessionService;

    @EventListener
    public void onCredentialsChanged(CredentialsChangedEvent event) {
        if (event.userId() == null) {
            return;
        }
        loginSessionService.revokeByUserId(event.userId());
    }
}
