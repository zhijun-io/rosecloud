package io.rosecloud.starter.security.auth.rest;

import io.rosecloud.common.security.model.UserPrincipal;
import io.rosecloud.starter.security.auth.BruteForceProtection;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

public class RestAuthenticationProvider implements AuthenticationProvider {

    private static final String BAD_CREDENTIALS = "用户名或密码错误";

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final BruteForceProtection bruteForce;

    public RestAuthenticationProvider(UserDetailsService userDetailsService,
                                      PasswordEncoder passwordEncoder,
                                      BruteForceProtection bruteForce) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.bruteForce = bruteForce;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.notNull(authentication, "No authentication data provided");

        if (!(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new BadCredentialsException(BAD_CREDENTIALS);
        }

        if (userPrincipal.getType() != UserPrincipal.Type.USER_NAME) {
            throw new BadCredentialsException(BAD_CREDENTIALS);
        }

        String username = userPrincipal.getValue();
        String password = (String) authentication.getCredentials();

        // H3: reject the attempt up-front if the account is locked from prior failures.
        bruteForce.assertNotLocked(username);

        // Deliberately uniform: a missing user, a wrong password and a disabled
        // account all surface the same exception so an attacker cannot enumerate
        // valid usernames or distinguish disabled from non-existent.
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (userDetails == null || !userDetails.isEnabled()
                || !passwordEncoder.matches(password, userDetails.getPassword())) {
            bruteForce.onFailure(username);
            throw new BadCredentialsException(BAD_CREDENTIALS);
        }

        bruteForce.onSuccess(username);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
