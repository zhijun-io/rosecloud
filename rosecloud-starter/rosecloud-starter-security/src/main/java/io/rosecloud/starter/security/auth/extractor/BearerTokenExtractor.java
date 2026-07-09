package io.rosecloud.starter.security.auth.extractor;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationServiceException;

public class BearerTokenExtractor implements TokenExtractor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public String extract(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new AuthenticationServiceException("Authorization header is missing or invalid");
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new AuthenticationServiceException("Authorization header is missing or invalid");
        }
        return token;
    }
}
