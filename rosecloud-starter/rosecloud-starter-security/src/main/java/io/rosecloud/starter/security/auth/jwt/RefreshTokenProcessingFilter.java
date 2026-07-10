 package io.rosecloud.starter.security.auth.jwt;
 
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.starter.security.auth.RefreshAuthenticationToken;
import io.rosecloud.starter.security.util.DeviceFingerprint;
import io.rosecloud.common.security.exception.AuthMethodNotSupportedException;
import io.rosecloud.common.security.token.RawAccessJwtToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

public class RefreshTokenProcessingFilter extends AbstractAuthenticationProcessingFilter {

    private static final String RAW_REFRESH_ATTR = "io.rosecloud.raw-refresh-token";

    private final AuthenticationSuccessHandler successHandler;
    private final AuthenticationFailureHandler failureHandler;
    private final ObjectMapper objectMapper;

    public RefreshTokenProcessingFilter(String defaultProcessUrl,
                                        AuthenticationSuccessHandler successHandler,
                                        AuthenticationFailureHandler failureHandler,
                                        ObjectMapper objectMapper) {
        super(defaultProcessUrl);
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.objectMapper = objectMapper;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        if (!HttpMethod.POST.name().equals(request.getMethod())) {
            throw new AuthMethodNotSupportedException("Authentication method not supported");
        }

        RefreshTokenRequest refreshTokenRequest;
        try {
            refreshTokenRequest = objectMapper.readValue(request.getReader(), RefreshTokenRequest.class);
        } catch (Exception e) {
            throw new AuthenticationServiceException("Invalid refresh token request payload");
        }

        if (refreshTokenRequest == null || refreshTokenRequest.refreshToken() == null
                || refreshTokenRequest.refreshToken().isBlank()) {
            throw new AuthenticationServiceException("Refresh token is not provided");
        }

        request.setAttribute(RAW_REFRESH_ATTR, refreshTokenRequest.refreshToken());
        RawAccessJwtToken token = new RawAccessJwtToken(refreshTokenRequest.refreshToken());
        return getAuthenticationManager().authenticate(new RefreshAuthenticationToken(token));
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult)
            throws IOException, ServletException {
        // M3: verify the presented refresh token's device fingerprint before minting new tokens.
        Object raw = request.getAttribute(RAW_REFRESH_ATTR);
        if (raw instanceof String rawToken && !DeviceFingerprint.verify(request, rawToken)) {
            SecurityContextHolder.clearContext();
            failureHandler.onAuthenticationFailure(request, response,
                    new BadCredentialsException("设备指纹校验失败"));
            return;
        }
        successHandler.onAuthenticationSuccess(request, response, authResult);
    }
 
     @Override
     protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                               AuthenticationException failed) throws IOException, ServletException {
         SecurityContextHolder.clearContext();
         failureHandler.onAuthenticationFailure(request, response, failed);
     }
 }
