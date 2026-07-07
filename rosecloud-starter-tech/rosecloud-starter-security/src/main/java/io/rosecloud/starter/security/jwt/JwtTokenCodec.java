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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Signs and verifies HS256 JWTs carrying the caller username. Used by the auth
 * service to issue access/refresh tokens and by servlet services to verify them
 * before hydrating the full caller context.
 *
 * <p>JSON (de)serialization runs through jjwt's gson bridge, so this codec is
 * independent of the host app's Jackson flavor (2 or 3).
 */
public class JwtTokenCodec {

    /**
     * Well-known dev-default secret committed to the repo (docker-compose). It must
     * never be used in production; the codec refuses it unless {@code allowDevSecret}
     * is explicitly opted in.
     */
    private static final String DEV_DEFAULT_SECRET = "rosecloud-dev-secret-please-change-me-0123456789";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenCodec(JwtProperties properties) {
        this.properties = properties;
        String secret = properties.getSecret();
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "rosecloud.security.jwt.secret must be at least 32 bytes for HS256");
        }
        if (DEV_DEFAULT_SECRET.equals(secret) && !properties.isAllowDevSecret()) {
            throw new IllegalStateException(
                    "Refusing to start with the committed dev-default JWT secret. "
                            + "Override rosecloud.security.jwt.secret with a strong random value, "
                            + "or set rosecloud.security.jwt.allow-dev-secret=true only for local dev.");
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
                .id(UUID.randomUUID().toString())
                .subject(user.username())
                .claim("type", type.name())
                .claim("perms", user.perms())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(key, Jwts.SIG.HS256);
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
                    subject(claims),
                    parseType(claims.get("type", String.class)),
                    claims.getId(),
                    claims.getExpiration() == null ? null : claims.getExpiration().toInstant(),
                    readPerms(claims));
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("invalid token", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> readPerms(Claims claims) {
        Object raw = claims.get("perms");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .map(String::valueOf)
                .filter(p -> !p.isBlank())
                .collect(Collectors.toList());
    }

    private static String subject(Claims claims) {
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new InvalidTokenException("missing token subject", null);
        }
        return subject;
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

}
