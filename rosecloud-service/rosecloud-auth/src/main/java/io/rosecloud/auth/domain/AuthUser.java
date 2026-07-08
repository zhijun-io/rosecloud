package io.rosecloud.auth.domain;

import io.rosecloud.common.core.model.HasStatus;
import io.rosecloud.common.core.model.HasTenantId;
import io.rosecloud.common.core.model.HasUserId;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Auth-domain view of a user. ORM-free so the credential source (Feign to the
 * system service today, something else later) can change behind the repository
 * port.
 */
public final class AuthUser implements HasUserId, HasStatus<Integer>, HasTenantId {

    private final Long userId;
    private final String username;
    private final String passwordHash;
    private final Integer status;
    private final Long tenantId;
    private final List<String> roles;
    private final List<String> perms;

    public AuthUser(Long userId, String username, String passwordHash, Integer status, Long tenantId,
                    List<String> roles, List<String> perms) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.status = status;
        this.tenantId = tenantId;
        this.roles = roles == null ? Collections.emptyList() : List.copyOf(roles);
        this.perms = perms == null ? Collections.emptyList() : List.copyOf(perms);
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Integer getStatus() {
        return status;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPerms() {
        return perms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthUser authUser)) return false;
        return Objects.equals(userId, authUser.userId) && Objects.equals(username, authUser.username)
                && Objects.equals(passwordHash, authUser.passwordHash) && Objects.equals(status, authUser.status)
                && Objects.equals(tenantId, authUser.tenantId) && Objects.equals(roles, authUser.roles)
                && Objects.equals(perms, authUser.perms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, username, passwordHash, status, tenantId, roles, perms);
    }

    @Override
    public String toString() {
        return "AuthUser[" +
                "userId=" + userId +
                ", username=" + username +
                ", passwordHash=" + passwordHash +
                ", status=" + status +
                ", tenantId=" + tenantId +
                ", roles=" + roles +
                ", perms=" + perms +
                ']';
    }
}
