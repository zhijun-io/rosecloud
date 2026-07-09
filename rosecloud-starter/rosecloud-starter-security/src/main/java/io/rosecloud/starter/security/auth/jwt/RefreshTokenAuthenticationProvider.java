package io.rosecloud.starter.security.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.session.SessionStore;
import io.rosecloud.common.security.token.RawAccessJwtToken;
import io.rosecloud.starter.security.auth.RefreshAuthenticationToken;
import io.rosecloud.starter.security.token.JwtTokenFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;
import java.util.function.Function;

public class RefreshTokenAuthenticationProvider implements AuthenticationProvider {

    private final JwtTokenFactory tokenFactory;
    private final SessionStore sessionStore;
    private final UserDetailsService userDetailsService;

    public RefreshTokenAuthenticationProvider(JwtTokenFactory tokenFactory,
                                              SessionStore sessionStore,
                                              UserDetailsService userDetailsService) {
        this.tokenFactory = tokenFactory;
        this.sessionStore = sessionStore;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        RawAccessJwtToken rawAccessToken = (RawAccessJwtToken) authentication.getCredentials();
        if (rawAccessToken == null || rawAccessToken.token() == null || rawAccessToken.token().isBlank()) {
            throw new BadCredentialsException("Refresh token is invalid");
        }

        Jws<Claims> jws = tokenFactory.parseRefreshToken(rawAccessToken.token());
        Claims claims = jws.getPayload();

        if (sessionStore.isRevoked(rawAccessToken.token())) {
            throw new BadCredentialsException("Refresh token is revoked");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
        if (userDetails == null) {
            throw new BadCredentialsException("User not found");
        }

        // A disabled account must not be able to mint fresh tokens from a still-valid
        // refresh token; mirrors the same guard on the access-token path
        // (JwtAuthenticationProvider) so disabling a user takes effect immediately.
        if (!userDetails.isEnabled()) {
            throw new BadCredentialsException("User is disabled");
        }

        // Refresh-token rotation: revoke the session bound to the presented (old) refresh
        // token so it cannot be reused after a new token pair is minted by the success handler.
        sessionStore.revoke(rawAccessToken.token());

        if (!(userDetails instanceof SecurityUser securityUser)) {
            throw new BadCredentialsException(
                    "Unsupported UserDetails type: " + userDetails.getClass().getName());
        }
        return new RefreshAuthenticationToken(securityUser);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return RefreshAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
