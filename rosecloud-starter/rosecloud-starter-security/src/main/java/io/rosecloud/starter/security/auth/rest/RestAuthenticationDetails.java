package io.rosecloud.starter.security.auth.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

public class RestAuthenticationDetails extends WebAuthenticationDetails {

    private static final long serialVersionUID = 1L;

    private final String userAgent;

    public RestAuthenticationDetails(HttpServletRequest request) {
        super(request);
        this.userAgent = request.getHeader("User-Agent");
    }

    public String getUserAgent() { return userAgent; }
}
