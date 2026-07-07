package io.rosecloud.auth.client;

import io.rosecloud.api.user.SystemUserApi;
import io.rosecloud.api.user.UserAuthInfo;
import io.rosecloud.auth.domain.AuthUser;
import io.rosecloud.auth.domain.UserRepository;
import io.rosecloud.common.core.model.ApiResponse;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Feign-backed {@link UserRepository}: resolves credentials and roles from the
 * system service instead of a local user table, so the auth service stays
 * persistence-free and the user store has a single owner. Feign errors (system
 * unavailable) propagate as runtime exceptions.
 */
@Repository
public class FeignUserRepository implements UserRepository {

    private final SystemUserApi systemUserApi;

    public FeignUserRepository(SystemUserApi systemUserApi) {
        this.systemUserApi = systemUserApi;
    }

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        ApiResponse<UserAuthInfo> response = systemUserApi.getAuthInfo(username);
        if (!response.success() || response.data() == null) {
            return Optional.empty();
        }
        UserAuthInfo info = response.data();
        return Optional.of(new AuthUser(info.userId(), info.username(), info.passwordHash(),
                info.status(), info.tenantId(), info.roles(), info.perms()));
    }
}
