package io.rosecloud.starter.security.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ClaimsBuilder;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import io.rosecloud.common.security.token.AccessJwtToken;
import io.rosecloud.common.security.token.JwtPair;
import io.rosecloud.starter.security.config.SecurityProperties;
import io.rosecloud.common.security.exception.JwtExpiredTokenException;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import org.springframework.security.authentication.BadCredentialsException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.UUID;

public class JwtTokenFactory implements io.rosecloud.common.security.token.TokenFactory {

    private static final String USER_ID = "userId";
    private static final String NICKNAME = "nickname";
    private static final String ENABLED = "enabled";
    private static final String TOKEN_TYPE = "type";
    private static final String TENANT = "tenant";
    private static final String AUDIENCE = "aud";
    private static final String FINGERPRINT = "fp";
    private static final String IMPERSONATION = "imp";

    /** Minimum secret length (bytes) recommended for HS512 (512 bits). */
    private static final int MIN_SECRET_BYTES = 64;

    private final SecurityProperties properties;
    private volatile JwtParser jwtParser;
    private volatile SecretKey secretKey;

    public JwtTokenFactory(SecurityProperties properties) {
        this.properties = properties;
        String secret = properties.getJwt().getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "rosecloud.security.jwt.secret must be set (env ROSECLOUD_SECURITY_JWT_SECRET). "
                            + "Refusing to start with a default/empty signing key.");
        }
        byte[] decoded = Base64.getDecoder().decode(secret);
        if (decoded.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("Configured rosecloud.security.jwt.secret decodes to "
                    + decoded.length + " bytes; HS512 requires at least " + MIN_SECRET_BYTES
                    + " bytes of high-entropy key.");
        }
    }

    public AccessJwtToken createAccessJwtToken(SecurityUser securityUser) {
        return createAccessJwtToken(securityUser, resolveActiveTenantId(securityUser.getTenantId()), null);
    }

    public AccessJwtToken createAccessJwtToken(SecurityUser securityUser, String activeTenantId) {
        return createAccessJwtToken(securityUser, activeTenantId, null);
    }

    public AccessJwtToken createAccessJwtToken(SecurityUser securityUser, String activeTenantId,
                                               String deviceFingerprint) {
        activeTenantId = resolveActiveTenantId(activeTenantId);
        ZonedDateTime now = ZonedDateTime.now();
        ClaimsBuilder claims = Jwts.claims()
                .subject(securityUser.getUsername())
                .id(UUID.randomUUID().toString())
                .add(USER_ID, securityUser.getUserId())
                .add(ENABLED, securityUser.isEnabled())
                .add(TENANT, activeTenantId);

        if (securityUser.isImpersonation()) {
            claims.add(IMPERSONATION, true);
        }

        if (securityUser.getNickname() != null) {
            claims.add(NICKNAME, securityUser.getNickname());
        }

        withAudienceAndFingerprint(claims, deviceFingerprint);

        String token = Jwts.builder()
                .claims(claims.build())
                .issuedAt(Date.from(now.toInstant()))
                .expiration(Date.from(now.plusSeconds(properties.getAccessTokenExpirationSeconds()).toInstant()))
                .issuer(properties.getJwt().getIssuer())
                .signWith(getSecretKey(), Jwts.SIG.HS512)
                .compact();

        return new AccessJwtToken(token);
    }

    public AccessJwtToken createRefreshToken(SecurityUser securityUser) {
        return createRefreshToken(securityUser, resolveActiveTenantId(securityUser.getTenantId()), null);
    }

    public AccessJwtToken createRefreshToken(SecurityUser securityUser, String activeTenantId) {
        return createRefreshToken(securityUser, activeTenantId, null);
    }

    public AccessJwtToken createRefreshToken(SecurityUser securityUser, String activeTenantId,
                                             String deviceFingerprint) {
        activeTenantId = resolveActiveTenantId(activeTenantId);
        ZonedDateTime now = ZonedDateTime.now();
        ClaimsBuilder claims = Jwts.claims()
                .subject(securityUser.getUsername())
                .id(UUID.randomUUID().toString())
                .add(USER_ID, securityUser.getUserId())
                .add(TOKEN_TYPE, "refresh")
                .add(TENANT, activeTenantId);

        if (securityUser.isImpersonation()) {
            claims.add(IMPERSONATION, true);
        }

        withAudienceAndFingerprint(claims, deviceFingerprint);

        String token = Jwts.builder()
                .claims(claims.build())
                .issuedAt(Date.from(now.toInstant()))
                .expiration(Date.from(now.plusSeconds(properties.getRefreshTokenExpirationSeconds()).toInstant()))
                .issuer(properties.getJwt().getIssuer())
                .signWith(getSecretKey(), Jwts.SIG.HS512)
                .compact();

        return new AccessJwtToken(token);
    }

    public Jws<Claims> parseAccessToken(String token) {
        Jws<Claims> jws = parseTokenClaims(token);
        // An access token must not be a refresh token. A null or "access" type is allowed;
        // only "refresh" is rejected here (L1: the previous check rejected ANY `type` claim,
        // which would have broken a future `type=access` convention).
        String type = jws.getPayload().get(TOKEN_TYPE, String.class);
        if ("refresh".equals(type)) {
            throw new BadCredentialsException("Invalid token type: expected access token");
        }
        return jws;
    }

    public Jws<Claims> parseRefreshToken(String token) {
        Jws<Claims> jws = parseTokenClaims(token);
        String type = jws.getPayload().get(TOKEN_TYPE, String.class);
        if (!"refresh".equals(type)) {
            throw new BadCredentialsException("Invalid token type: expected refresh token");
        }
        return jws;
    }

    public JwtPair createTokenPair(SecurityUser securityUser) {
        return createTokenPair(securityUser, resolveActiveTenantId(securityUser.getTenantId()), null);
    }

    public JwtPair createTokenPair(SecurityUser securityUser, String activeTenantId) {
        return createTokenPair(securityUser, activeTenantId, null);
    }

    public JwtPair createTokenPair(SecurityUser securityUser, String activeTenantId, String deviceFingerprint) {
        AccessJwtToken access = createAccessJwtToken(securityUser, activeTenantId, deviceFingerprint);
        AccessJwtToken refresh = createRefreshToken(securityUser, activeTenantId, deviceFingerprint);
        return new JwtPair(access.token(), refresh.token());
    }

    private void withAudienceAndFingerprint(ClaimsBuilder claims, String deviceFingerprint) {
        String aud = audience();
        if (aud != null) {
            // M2: audience claim. Set as a plain claim to stay version-independent; it is
            // verified in parseTokenClaims via requireAudience.
            claims.add(AUDIENCE, aud);
        }
        if (deviceFingerprint != null && !deviceFingerprint.isBlank()) {
            claims.add(FINGERPRINT, deviceFingerprint);
        }
    }

    private String audience() {
        String aud = properties.getJwt().getAudience();
        return (aud == null || aud.isBlank()) ? null : aud;
    }

    public Jws<Claims> parseTokenClaims(String token) {
        try {
            Jws<Claims> jws = getJwtParser().parseSignedClaims(token);
            requireAudience(jws);
            return jws;
        } catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Invalid JWT token", ex);
        } catch (SignatureException ex) {
            throw new BadCredentialsException("Invalid JWT signature", ex);
        } catch (ExpiredJwtException ex) {
            throw new JwtExpiredTokenException("JWT token expired", ex);
        }
    }

    private void requireAudience(Jws<Claims> jws) {
        String expected = audience();
        if (expected == null) {
            return;
        }
        Set<String> audiences = jws.getPayload().getAudience();
        if (audiences == null || audiences.stream().noneMatch(expected::equals)) {
            throw new BadCredentialsException("Invalid JWT audience");
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return properties.getAccessTokenExpirationSeconds();
    }

    private SecretKey getSecretKey() {
        if (secretKey == null) {
            synchronized (this) {
                if (secretKey == null) {
                    byte[] decoded = Base64.getDecoder().decode(properties.getJwt().getSecret());
                    // Same key material for both signing and verification, so behaviour is
                    // consistent regardless of secret length.
                    secretKey = new SecretKeySpec(decoded, "HmacSHA512");
                }
            }
        }
        return secretKey;
    }

    private JwtParser getJwtParser() {
        if (jwtParser == null) {
            synchronized (this) {
                if (jwtParser == null) {
                    jwtParser = Jwts.parser()
                            .verifyWith(getSecretKey())
                            .requireIssuer(properties.getJwt().getIssuer())
                            .build();
                }
            }
        }
        return jwtParser;
    }

    private static String resolveActiveTenantId(String tenantId) {
        return (tenantId == null || tenantId.isBlank())
                ? TenantContextHolder.SYSTEM_TENANT_ID
                : tenantId.trim().toUpperCase(Locale.ROOT);
    }

}
