package io.rosecloud.starter.security.auth.jwt;

import io.rosecloud.starter.security.token.BearerTokenExtractor;
import io.rosecloud.starter.security.auth.AbstractJwtAuthenticationToken;
import io.rosecloud.starter.security.auth.JwtAuthenticationToken;
import io.rosecloud.starter.security.util.DeviceFingerprint;
import io.rosecloud.common.security.token.RawAccessJwtToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;

public class JwtTokenAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter {

    private final AuthenticationFailureHandler failureHandler;
    private final BearerTokenExtractor tokenExtractor;

    public JwtTokenAuthenticationProcessingFilter(RequestMatcher matcher,
                                                  AuthenticationFailureHandler failureHandler,
                                                  BearerTokenExtractor tokenExtractor) {
        super(matcher);
        this.failureHandler = failureHandler;
        this.tokenExtractor = tokenExtractor;
    }

    private static final String RAW_TOKEN_ATTR = "io.rosecloud.raw-access-token";

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        String raw = tokenExtractor.extract(request);
        request.setAttribute(RAW_TOKEN_ATTR, raw);
        RawAccessJwtToken token = new RawAccessJwtToken(raw);
        return getAuthenticationManager().authenticate(new JwtAuthenticationToken(token));
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult)
            throws IOException, ServletException {
        // M3: a token minted with device binding carries an `fp` claim; if it does, the claim
        // must match this request's fingerprint or the (stolen) token is rejected.
        Object raw = request.getAttribute(RAW_TOKEN_ATTR);
        if (raw instanceof String rawToken && !DeviceFingerprint.verify(request, rawToken)) {
            SecurityContextHolder.clearContext();
            failureHandler.onAuthenticationFailure(request, response,
                    new BadCredentialsException("设备指纹校验失败"));
            return;
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);
        if (raw instanceof String s && authResult instanceof AbstractJwtAuthenticationToken token) {
            token.setDetails(s);
        }
        chain.doFilter(request, response);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        failureHandler.onAuthenticationFailure(request, response, failed);
    }
}
