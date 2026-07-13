package io.rosecloud.common.security.credential;

import java.time.LocalDateTime;

/**
 * Read model for a user's auth-owned credential, returned by auth's {@code CredentialService}.
 * The password hash is only ever produced and compared inside the auth service.
 */
public record AuthCredential(Long userId, String passwordHash, LocalDateTime passwordChangedTime,
                             boolean enabled, LocalDateTime lastLoginTime) {
}
