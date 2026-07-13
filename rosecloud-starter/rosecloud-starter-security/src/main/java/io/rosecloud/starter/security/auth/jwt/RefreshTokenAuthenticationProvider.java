package io.rosecloud.starter.security.auth.jwt;
import lombok.RequiredArgsConstructor;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.rosecloud.api.user.TenantLookupApi;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.session.SessionStore;
import io.rosecloud.common.security.token.RawAccessJwtToken;
import io.rosecloud.starter.security.auth.BruteForceProtection;
import io.rosecloud.starter.security.auth.RefreshAuthenticationToken;
import io.rosecloud.starter.security.token.JwtTokenFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Locale;

@RequiredArgsConstructor
public class RefreshTokenAuthenticationProvider implements AuthenticationProvider {

    private final JwtTokenFactory tokenFactory;
    private final SessionStore sessionStore;
    private final UserDetailsService userDetailsService;
    private final TenantLookupApi tenantLookupApi;
    private final BruteForceProtection bruteForce;
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        RawAccessJwtToken rawAccessToken = (RawAccessJwtToken) authentication.getCredentials();
        JwtAuthSupport.requireValidRawToken(rawAccessToken, "Refresh token is invalid");

        Jws<Claims> jws = tokenFactory.parseRefreshToken(rawAccessToken.token());
        Claims claims = jws.getPayload();
        String subject = claims.getSubject();

        // H3: throttle refresh attempts per account, mirroring the login provider.
        bruteForce.assertNotLocked(subject);
        try {
            if (sessionStore.isRevoked(rawAccessToken.token())) {
                throw new BadCredentialsException("Refresh token is revoked");
            }

            // A disabled account must not be able to mint fresh tokens from a still-valid
            // refresh token; mirrors the same guard on the access-token path
            // (JwtAuthenticationProvider) so disabling a user takes effect immediately.
            SecurityUser securityUser = JwtAuthSupport.loadAndValidateUser(claims.getSubject(), userDetailsService);

            // Refresh-token rotation: revoke the session bound to the presented (old) refresh
            // token so it cannot be reused after a new token pair is minted by the success handler.
            sessionStore.revoke(rawAccessToken.token());

            // Preserve the active tenant carried by the refresh token so the refreshed access
            // token keeps the same tenant scope (no implicit switch on refresh).
            String tokenTenant = claims.get("tenant", String.class);
            String effectiveTenant = (tokenTenant == null || tokenTenant.isBlank())
                    ? JwtAuthSupport.normalizeTenantId(securityUser.getTenantId())
                    : tokenTenant.trim().toUpperCase(Locale.ROOT);
            TenantStatusChecks.requireEnabled(effectiveTenant, tenantLookupApi);
            SecurityUser effectiveUser = securityUser.withTenantId(effectiveTenant);
            bruteForce.onSuccess(subject);
            return new RefreshAuthenticationToken(effectiveUser);
        } catch (AuthenticationException e) {
            bruteForce.onFailure(subject);
            throw e;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return RefreshAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
