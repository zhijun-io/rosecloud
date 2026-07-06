package io.rosecloud.starter.security.jwt;

/**
 * Raised when a JWT is missing, malformed, expired, or fails signature/issuer
 * verification. Callers (gateway/auth) translate it to a 401.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
