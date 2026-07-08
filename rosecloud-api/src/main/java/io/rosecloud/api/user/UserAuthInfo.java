package io.rosecloud.api.user;

import java.util.List;
import java.time.LocalDateTime;

/**
 * Auth-facing snapshot of a user, returned by the system service to the auth
 * service over Feign. Carries the password hash from the credential table so
 * the auth service can verify credentials locally; never exposed through the
 * gateway (the internal endpoint is reachable only via direct service-to-service calls).
 *
 * <p>The {@code username} field is the login identifier, not a dedicated
 * {@code sys_user.username} column. {@code passwordChangedTime} marks the
 * boundary after which older JWTs should be treated as stale.
 */
public record UserAuthInfo(Long userId, String username, String passwordHash,
                           Integer status, String tenantId, List<String> roles, List<String> perms,
                           LocalDateTime passwordChangedTime) {

    public UserAuthInfo {
        perms = perms == null ? List.of() : List.copyOf(perms);
    }
}
