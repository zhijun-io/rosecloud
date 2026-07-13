package io.rosecloud.starter.security.auth.rest;

import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.common.core.util.JacksonUtil;
import io.rosecloud.common.security.exception.AuthMethodNotSupportedException;
import io.rosecloud.common.security.model.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

public class RestLoginProcessingFilter extends AbstractAuthenticationProcessingFilter {

    private static final String PROCESSING_URL = ServiceMetadata.API_PREFIX + "/auth/login";

    private final AuthenticationSuccessHandler successHandler;
    private final AuthenticationFailureHandler failureHandler;

    public RestLoginProcessingFilter(AuthenticationSuccessHandler successHandler,
                                     AuthenticationFailureHandler failureHandler) {
        super(PROCESSING_URL);
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {
        if (!HttpMethod.POST.name().equals(request.getMethod())) {
            throw new AuthMethodNotSupportedException("Authentication method not supported");
        }

        LoginRequest loginRequest;
        try {
            loginRequest = JacksonUtil.getObjectMapper().readValue(request.getReader(), LoginRequest.class);
        } catch (Exception e) {
            throw new AuthenticationServiceException("Invalid login request payload", e);
        }

        if (loginRequest.getUsername() == null || loginRequest.getUsername().isBlank()
                || loginRequest.getPassword() == null || loginRequest.getPassword().isBlank()) {
            throw new AuthenticationServiceException("Username or Password not provided");
        }

        request.setAttribute("ATTEMPTED_USERNAME", loginRequest.getUsername());

        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, loginRequest.getUsername());
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                principal, loginRequest.getPassword());
        return getAuthenticationManager().authenticate(token);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult)
            throws IOException, ServletException {
        successHandler.onAuthenticationSuccess(request, response, authResult);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        failureHandler.onAuthenticationFailure(request, response, failed);
    }
}
