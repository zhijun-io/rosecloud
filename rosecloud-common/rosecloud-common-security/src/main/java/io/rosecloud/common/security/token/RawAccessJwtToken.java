package io.rosecloud.common.security.token;

public record RawAccessJwtToken(String token) implements JwtToken {}
