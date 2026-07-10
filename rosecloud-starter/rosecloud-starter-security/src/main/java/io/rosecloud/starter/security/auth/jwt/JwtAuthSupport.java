package io.rosecloud.starter.security.auth.jwt;

import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.token.RawAccessJwtToken;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Locale;

/**
 * Shared validation steps for the JWT and refresh-token authentication providers,
 * which otherwise repeat the same raw-token sanity check, user lookup, enabled
 * check, and {@link SecurityUser} cast.
 */
final class JwtAuthSupport {

    private JwtAuthSupport() {
    }

    static void requireValidRawToken(RawAccessJwtToken rawAccessToken, String invalidMessage) {
        if (rawAccessToken == null || rawAccessToken.token() == null || rawAccessToken.token().isBlank()) {
            throw new BadCredentialsException(invalidMessage);
        }
    }

    static SecurityUser loadAndValidateUser(String username, UserDetailsService userDetailsService) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (userDetails == null) {
            throw new BadCredentialsException("User not found");
        }
        if (!userDetails.isEnabled()) {
            throw new BadCredentialsException("User is disabled");
        }
        if (!(userDetails instanceof SecurityUser securityUser)) {
            throw new BadCredentialsException(
                    "Unsupported UserDetails type: " + userDetails.getClass().getName());
        }
        return securityUser;
    }

    static String normalizeTenantId(String tenantId) {
        return (tenantId == null || tenantId.isBlank())
                ? TenantContextHolder.SYSTEM_TENANT_ID
                : tenantId.trim().toUpperCase(Locale.ROOT);
    }
}
