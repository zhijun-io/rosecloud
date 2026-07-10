package io.rosecloud.starter.security.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.rosecloud.common.security.exception.JwtExpiredTokenException;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.token.RawAccessJwtToken;
import io.rosecloud.starter.security.auth.JwtAuthenticationToken;
import io.rosecloud.starter.security.token.JwtTokenFactory;
import io.rosecloud.common.security.session.SessionStore;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;

public class JwtAuthenticationProvider implements AuthenticationProvider {

    private final JwtTokenFactory tokenFactory;
    private final SessionStore sessionStore;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationProvider(JwtTokenFactory tokenFactory, SessionStore sessionStore,
                                     UserDetailsService userDetailsService) {
        this.tokenFactory = tokenFactory;
        this.sessionStore = sessionStore;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        RawAccessJwtToken rawAccessToken = (RawAccessJwtToken) authentication.getCredentials();
        JwtAuthSupport.requireValidRawToken(rawAccessToken, "Token is invalid");

        Jws<Claims> jws = tokenFactory.parseAccessToken(rawAccessToken.token());
        Claims claims = jws.getPayload();

        if (sessionStore.isRevoked(rawAccessToken.token())) {
            throw new JwtExpiredTokenException("Token is outdated");
        }

        SecurityUser securityUser = JwtAuthSupport.loadAndValidateUser(claims.getSubject(), userDetailsService);

        // The active tenant travels in the token's signed "tenant" claim (set at login or
        // after a tenant switch). For legacy tokens that predate the claim, fall back to the
        // principal's home tenant — and treat a null home tenant (platform admin) as the
        // system tenant. Trusted because the token is signature-verified.
        String tokenTenant = claims.get("tenant", String.class);
        if (tokenTenant == null || tokenTenant.isBlank()) {
            tokenTenant = securityUser.getTenantId() != null
                    ? securityUser.getTenantId()
                    : io.rosecloud.starter.tenant.core.TenantContextHolder.SYSTEM_TENANT_ID;
        }
        SecurityUser effectiveUser = securityUser.withTenantId(tokenTenant);
        return new JwtAuthenticationToken(effectiveUser);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
