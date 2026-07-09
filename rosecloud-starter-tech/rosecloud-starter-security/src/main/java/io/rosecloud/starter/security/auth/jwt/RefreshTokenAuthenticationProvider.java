package io.rosecloud.starter.security.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.rosecloud.starter.security.auth.RefreshAuthenticationToken;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.starter.security.token.JwtTokenFactory;
import io.rosecloud.common.security.token.RawAccessJwtToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class RefreshTokenAuthenticationProvider implements AuthenticationProvider {

    private final JwtTokenFactory tokenFactory;
    private final UserDetailsService userDetailsService;

    public RefreshTokenAuthenticationProvider(JwtTokenFactory tokenFactory,
                                              UserDetailsService userDetailsService) {
        this.tokenFactory = tokenFactory;
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

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
            return new RefreshAuthenticationToken((SecurityUser) userDetails);
        } catch (UsernameNotFoundException e) {
            throw new BadCredentialsException("User not found", e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return RefreshAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
