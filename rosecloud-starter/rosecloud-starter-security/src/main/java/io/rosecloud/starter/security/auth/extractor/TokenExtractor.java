package io.rosecloud.starter.security.auth.extractor;

import jakarta.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface TokenExtractor {
    String extract(HttpServletRequest request);
}
