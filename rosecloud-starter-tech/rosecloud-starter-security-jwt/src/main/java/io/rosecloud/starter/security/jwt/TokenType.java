package io.rosecloud.starter.security.jwt;

/** Distinguishes access tokens from refresh tokens via the {@code type} claim. */
public enum TokenType {
    ACCESS,
    REFRESH
}
