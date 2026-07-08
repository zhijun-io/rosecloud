package io.rosecloud.system.domain;

import java.util.Objects;

/**
 * Provisioning credentials captured at tenant apply time and consumed once when
 * the tenant is opened. The password is stored hashed and cleared after the
 * first admin is created.
 */
public final class TenantAdminCredentials {

    private final String username;
    private final String passwordHash;

    public TenantAdminCredentials(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TenantAdminCredentials that)) return false;
        return Objects.equals(username, that.username) && Objects.equals(passwordHash, that.passwordHash);
    }

    @Override
    public int hashCode() { return Objects.hash(username, passwordHash); }

    @Override
    public String toString() {
        return "TenantAdminCredentials[" + "username=" + username + ", passwordHash=" + passwordHash + ']';
    }
}
