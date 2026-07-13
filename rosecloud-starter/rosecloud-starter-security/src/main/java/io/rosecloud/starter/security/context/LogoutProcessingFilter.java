package io.rosecloud.starter.security.context;

import io.rosecloud.common.core.util.JacksonUtil;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.starter.security.token.BearerTokenExtractor;
import io.rosecloud.starter.security.session.LoginSessionApi;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class LogoutProcessingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LogoutProcessingFilter.class);

    private final RequestMatcher matcher = PathPatternRequestMatcher.pathPattern(HttpMethod.POST,
            ServiceMetadata.API_PREFIX + "/auth/logout");

    private final BearerTokenExtractor tokenExtractor;
    private final LoginSessionApi loginSessionApi;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!matcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = tokenExtractor.extract(request);
            loginSessionApi.revoke(token);
        } catch (Exception ex) {
            log.debug("Logout token parse/revoke failed; clearing context anyway", ex);
        }

        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        JacksonUtil.getObjectMapper().writeValue(response.getWriter(), ApiResponse.ok());
    }
}
