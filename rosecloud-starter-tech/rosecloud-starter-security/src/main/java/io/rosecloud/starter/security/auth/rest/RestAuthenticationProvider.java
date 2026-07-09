package io.rosecloud.starter.security.auth.rest;

import io.rosecloud.common.security.model.UserPrincipal;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

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

        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            throw new BadCredentialsException("Bad credentials");
        }
        if (userDetails == null) {
            throw new BadCredentialsException("Bad credentials");
        }

        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Bad credentials");
        }

        if (!userDetails.isEnabled()) {
            throw new BadCredentialsException("User is disabled");
        }

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
