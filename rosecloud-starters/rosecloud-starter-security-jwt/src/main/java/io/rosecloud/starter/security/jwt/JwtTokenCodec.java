package io.rosecloud.starter.security.jwt;

import io.rosecloud.common.security.context.CurrentUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Signs and verifies HS256 JWTs carrying the caller identity. Used by the auth
 * service to issue access/refresh tokens and by the gateway to verify them and
 * rebuild the identity for header injection.
 *
 * <p>JSON (de)serialization runs through jjwt's gson bridge, so this codec is
 * independent of the host app's Jackson flavor (2 or 3).
 */
public class JwtTokenCodec {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenCodec(JwtProperties properties) {
        this.properties = properties;
        byte[] secretBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "rosecloud.jwt.secret must be at least 32 bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String issueAccessToken(CurrentUser user) {
        return issue(user, TokenType.ACCESS, properties.getAccessTtl().toMillis());
    }

    public String issueRefreshToken(CurrentUser user) {
        return issue(user, TokenType.REFRESH, properties.getRefreshTtl().toMillis());
    }

    private String issue(CurrentUser user, TokenType type, long ttlMillis) {
        Date now = new Date();
        var builder = Jwts.builder()
                .issuer(properties.getIssuer())
                .claim("username", user.username())
                .claim("roles", user.roles())
                .claim("type", type.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(key, Jwts.SIG.HS256);
        if (user.userId() != null) {
            builder.subject(String.valueOf(user.userId()))
                    .claim("uid", String.valueOf(user.userId()));
        }
        if (user.tenantId() != null) {
            builder.claim("tid", String.valueOf(user.tenantId()));
        }
        return builder.compact();
    }

    /**
     * Verifies signature, issuer and expiry, returning the decoded claims.
     *
     * @throws InvalidTokenException if the token is missing, malformed, expired,
     *                               or fails verification
     */
    public TokenClaims parse(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("missing bearer token", null);
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new TokenClaims(
                    parseLong(claims.get("uid", String.class)),
                    claims.get("username", String.class),
                    parseLong(claims.get("tid", String.class)),
                    roles(claims),
                    parseType(claims.get("type", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("invalid token", e);
        }
    }

    private static TokenType parseType(String value) {
        if (value == null) {
            throw new InvalidTokenException("missing token type", null);
        }
        try {
            return TokenType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("invalid token type", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> roles(Claims claims) {
        Object raw = claims.get("roles");
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
