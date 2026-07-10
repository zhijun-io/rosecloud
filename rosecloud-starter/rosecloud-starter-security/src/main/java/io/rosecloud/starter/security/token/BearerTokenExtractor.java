package io.rosecloud.starter.security.token;

import io.rosecloud.common.security.SecurityHeaders;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationServiceException;

public class BearerTokenExtractor {

    public String extract(HttpServletRequest request) {
        String header = request.getHeader(SecurityHeaders.AUTHORIZATION);
        if (header == null
                || !header.regionMatches(true, 0, SecurityHeaders.BEARER_PREFIX, 0, SecurityHeaders.BEARER_PREFIX.length())) {
            throw new AuthenticationServiceException("Authorization header is missing or invalid");
        }
        String token = header.substring(SecurityHeaders.BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new AuthenticationServiceException("Authorization header is missing or invalid");
        }
        return token;
    }
}
