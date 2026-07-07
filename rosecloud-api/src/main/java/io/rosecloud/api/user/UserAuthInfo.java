package io.rosecloud.api.user;

import java.util.List;

/**
 * Auth-facing snapshot of a user, returned by the system service to the auth
 * service over Feign. Carries the password hash so the auth service can verify
 * credentials locally; never exposed through the gateway (the internal endpoint
 * is reachable only via direct service-to-service calls).
 */
public record UserAuthInfo(Long userId, String username, String passwordHash,
                           Integer status, Long tenantId, List<String> roles, List<String> perms) {

    public UserAuthInfo {
        perms = perms == null ? List.of() : List.copyOf(perms);
    }
}
