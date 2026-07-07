package io.rosecloud.system.domain;

/**
 * Provisioning credentials captured at tenant apply time and consumed once when
 * the tenant is opened. The password is stored hashed and cleared after the
 * first admin is created.
 */
public record TenantAdminCredentials(String username, String passwordHash) {
}
