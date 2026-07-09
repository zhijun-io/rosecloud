package io.rosecloud.common.security.exception;

import org.springframework.security.authentication.AuthenticationServiceException;

public class AuthMethodNotSupportedException extends AuthenticationServiceException {

    private static final long serialVersionUID = 1L;

    public AuthMethodNotSupportedException(String msg) {
        super(msg);
    }
}
