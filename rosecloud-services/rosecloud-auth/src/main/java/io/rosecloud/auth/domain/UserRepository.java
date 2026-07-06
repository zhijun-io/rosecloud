package io.rosecloud.auth.domain;

import java.util.Optional;

/**
 * Repository port for user credentials. Implemented in the infrastructure layer;
 * the service depends only on this interface.
 */
public interface UserRepository {

    Optional<AuthUser> findByUsername(String username);
}
