package io.rosecloud.common.security.token;

public record AccessJwtToken(String token) implements JwtToken {}
