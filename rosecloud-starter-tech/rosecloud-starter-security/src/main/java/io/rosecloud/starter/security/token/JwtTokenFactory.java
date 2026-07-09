package io.rosecloud.starter.security.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ClaimsBuilder;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.rosecloud.common.security.token.AccessJwtToken;
import io.rosecloud.common.security.token.JwtPair;
import io.rosecloud.starter.security.config.SecurityProperties;
import io.rosecloud.common.security.exception.JwtExpiredTokenException;
import io.rosecloud.common.security.model.SecurityUser;
import org.springframework.security.authentication.BadCredentialsException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

public class JwtTokenFactory implements io.rosecloud.common.security.token.TokenFactory {

    private static final String USER_ID = "userId";
    private static final String NICKNAME = "nickname";
    private static final String ENABLED = "enabled";
    private static final String TOKEN_TYPE = "type";

    private final SecurityProperties properties;
    private volatile JwtParser jwtParser;
    private volatile SecretKey secretKey;

    public JwtTokenFactory(SecurityProperties properties) {
        this.properties = properties;
    }

    public AccessJwtToken createAccessJwtToken(SecurityUser securityUser) {
        ZonedDateTime now = ZonedDateTime.now();
        ClaimsBuilder claims = Jwts.claims()
                .subject(securityUser.getUsername())
                .id(UUID.randomUUID().toString())
                .add(USER_ID, securityUser.getUserId())
                .add(ENABLED, securityUser.isEnabled());

        if (securityUser.getNickname() != null) {
            claims.add(NICKNAME, securityUser.getNickname());
        }

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
        ZonedDateTime now = ZonedDateTime.now();
        ClaimsBuilder claims = Jwts.claims()
                .subject(securityUser.getUsername())
                .id(UUID.randomUUID().toString())
                .add(USER_ID, securityUser.getUserId())
                .add(TOKEN_TYPE, "refresh");

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
        if (jws.getPayload().get(TOKEN_TYPE, String.class) != null) {
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
        AccessJwtToken access = createAccessJwtToken(securityUser);
        AccessJwtToken refresh = createRefreshToken(securityUser);
        return new JwtPair(access.token(), refresh.token());
    }

    public Jws<Claims> parseTokenClaims(String token) {
        try {
            return getJwtParser().parseSignedClaims(token);
        } catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Invalid JWT token", ex);
        } catch (SignatureException ex) {
            throw new BadCredentialsException("Invalid JWT signature", ex);
        } catch (ExpiredJwtException ex) {
            throw new JwtExpiredTokenException("JWT token expired", ex);
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
                            .verifyWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(properties.getJwt().getSecret())))
                            .build();
                }
            }
        }
        return jwtParser;
    }

}
