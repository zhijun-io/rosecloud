package io.rosecloud.common.security.exception;

import org.springframework.security.core.AuthenticationException;

public class JwtExpiredTokenException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    public JwtExpiredTokenException(String msg) {
        super(msg);
    }

    public JwtExpiredTokenException(String msg, Throwable t) {
        super(msg, t);
    }
}
