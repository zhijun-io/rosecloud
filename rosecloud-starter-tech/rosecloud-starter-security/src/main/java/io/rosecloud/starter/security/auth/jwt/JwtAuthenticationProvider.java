 package io.rosecloud.starter.security.auth.jwt;
 
 import io.jsonwebtoken.Claims;
 import io.jsonwebtoken.Jws;
 import io.rosecloud.starter.security.auth.JwtAuthenticationToken;
 import io.rosecloud.common.security.exception.JwtExpiredTokenException;
 import io.rosecloud.common.security.model.SecurityUser;
 import io.rosecloud.starter.security.token.JwtTokenFactory;
 import io.rosecloud.common.security.token.RawAccessJwtToken;
import io.rosecloud.common.security.session.SessionStore;
import org.springframework.security.authentication.AuthenticationProvider;
 import org.springframework.security.authentication.BadCredentialsException;
 import org.springframework.security.core.Authentication;
 import org.springframework.security.core.AuthenticationException;
 import org.springframework.security.core.userdetails.UserDetails;
 import org.springframework.security.core.userdetails.UserDetailsService;
 import org.springframework.security.core.userdetails.UsernameNotFoundException;
 
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
         if (rawAccessToken == null || rawAccessToken.token() == null || rawAccessToken.token().isBlank()) {
             throw new BadCredentialsException("Token is invalid");
         }
 
         Jws<Claims> jws = tokenFactory.parseAccessToken(rawAccessToken.token());
         Claims claims = jws.getPayload();
 
        if (sessionStore.isRevoked(rawAccessToken.token())) {
             throw new JwtExpiredTokenException("Token is outdated");
         }
 
         UserDetails userDetails;
         try {
             userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
         } catch (UsernameNotFoundException e) {
             throw new BadCredentialsException("User not found", e);
         }
 
         if (!userDetails.isEnabled()) {
             throw new BadCredentialsException("User is disabled");
         }
 
         return new JwtAuthenticationToken((SecurityUser) userDetails);
     }
 
     @Override
     public boolean supports(Class<?> authentication) {
         return JwtAuthenticationToken.class.isAssignableFrom(authentication);
     }
 }
