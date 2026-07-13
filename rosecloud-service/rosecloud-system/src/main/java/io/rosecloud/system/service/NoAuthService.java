package io.rosecloud.system.service;

import io.rosecloud.system.service.dto.ActivationResult;
import io.rosecloud.system.service.dto.UserActivationInfo;

/**
 * Handles the no-auth (public) activation flow: token verification, full activation
 * with token/session/login-log creation, and resend.
 */
public interface NoAuthService {

    /** Checks whether an activation token is still valid. */
    UserActivationInfo check(String activateToken);

    /**
     * Completes the activation workflow: confirms the token, creates JWT tokens,
     * saves a login session, records a login log entry, and returns the token pair.
     */
    ActivationResult activate(String activateToken, String password, String ip, String userAgent);

    /** Resends the activation email for the given username. */
    void resend(String username);
}
