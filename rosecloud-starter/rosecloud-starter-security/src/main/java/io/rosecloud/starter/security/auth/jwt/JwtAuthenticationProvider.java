package io.rosecloud.starter.security.auth.jwt;
import lombok.RequiredArgsConstructor;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.rosecloud.api.user.TenantLookupApi;
import io.rosecloud.common.security.exception.JwtExpiredTokenException;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.token.RawAccessJwtToken;
import io.rosecloud.starter.security.auth.JwtAuthenticationToken;
import io.rosecloud.starter.security.token.JwtTokenFactory;
import io.rosecloud.starter.security.session.LoginSessionApi;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private final JwtTokenFactory tokenFactory;
    private final LoginSessionApi loginSessionApi;
    private final TenantLookupApi tenantLookupApi;
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        RawAccessJwtToken rawAccessToken = (RawAccessJwtToken) authentication.getCredentials();
        JwtAuthSupport.requireValidRawToken(rawAccessToken, "Token is invalid");

        Jws<Claims> jws = tokenFactory.parseAccessToken(rawAccessToken.token());
        Claims claims = jws.getPayload();

        if (loginSessionApi.isRevoked(rawAccessToken.token())) {
            throw new JwtExpiredTokenException("Token is outdated");
        }

        // Reconstruct the principal directly from the signed claims. Authorities were baked
        // into the token at login (Slice C / AC-7), so no per-request lookup back to system is
        // needed for authorization. The token is signature-verified, so its claims are trusted.
        boolean enabled = Boolean.TRUE.equals(claims.get("enabled", Boolean.class));
        if (!enabled) {
            throw new BadCredentialsException("认证失败");
        }
        List<String> authorityStrings = claims.get("authorities", List.class);
        Number userId = (Number) claims.get("userId");
        SecurityUser securityUser = SecurityUser.fromJson(
                userId != null ? userId.longValue() : null,
                claims.getSubject(),
                claims.get("nickname", String.class),
                null,
                enabled,
                null,
                null,
                authorityStrings);

        // The active tenant travels in the token's signed "tenant" claim (set at login or
        // after a tenant switch). For legacy tokens that predate the claim, fall back to the
        // principal's home tenant — and treat a null home tenant (platform admin) as the
        // system tenant. Trusted because the token is signature-verified.
        String tokenTenant = claims.get("tenant", String.class);
        if (tokenTenant == null || tokenTenant.isBlank()) {
            tokenTenant = JwtAuthSupport.normalizeTenantId(securityUser.getTenantId());
        } else {
            tokenTenant = tokenTenant.trim().toUpperCase(Locale.ROOT);
        }
        TenantStatusChecks.requireEnabled(tokenTenant, tenantLookupApi);
        SecurityUser effectiveUser = securityUser.withTenantId(tokenTenant);

        // Apply impersonation flag from the token claim.
        Boolean impersonationClaim = claims.get("imp", Boolean.class);
        if (Boolean.TRUE.equals(impersonationClaim)) {
            effectiveUser = effectiveUser.withImpersonation(true);
        }
        return new JwtAuthenticationToken(effectiveUser);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
