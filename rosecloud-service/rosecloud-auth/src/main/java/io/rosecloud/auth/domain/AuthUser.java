package io.rosecloud.auth.domain;

import java.util.Collections;
import java.util.List;

/**
 * Auth-domain view of a user. ORM-free so the credential source (Feign to the
 * system service today, something else later) can change behind the repository
 * port.
 */
public record AuthUser(Long userId, String username, String passwordHash,
                       Integer status, Long tenantId, List<String> roles, List<String> perms) {

    public AuthUser {
        roles = roles == null ? Collections.emptyList() : List.copyOf(roles);
        perms = perms == null ? Collections.emptyList() : List.copyOf(perms);
    }
}
