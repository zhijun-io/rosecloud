package io.rosecloud.starter.security.auth.rest;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.security.model.UserPrincipal;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

import static io.rosecloud.common.security.exception.SecurityErrorCode.*;

public class RestAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public RestAuthenticationProvider(UserDetailsService userDetailsService,
                                      PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.notNull(authentication, "No authentication data provided");

        if (!(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new BadCredentialsException("Authentication Failed. Bad user principal.");
        }

        if (userPrincipal.getType() != UserPrincipal.Type.USER_NAME) {
            throw new BadCredentialsException("Authentication Failed. Unsupported principal type.");
        }

        String username = userPrincipal.getValue();
        String password = (String) authentication.getCredentials();

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (userDetails == null) {
            throw new BizException(USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BizException(BAD_CREDENTIALS);
        }

        if (!userDetails.isEnabled()) {
            throw new BizException(USER_DISABLED);
        }

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
