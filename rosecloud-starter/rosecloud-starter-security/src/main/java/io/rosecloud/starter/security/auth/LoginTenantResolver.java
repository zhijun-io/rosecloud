package io.rosecloud.starter.security.auth;

import io.rosecloud.common.security.model.SecurityUser;

/**
 * Resolves the tenant that should become active when a user logs in.
 */
public interface LoginTenantResolver {

    String resolveInitialTenant(SecurityUser securityUser);
}
