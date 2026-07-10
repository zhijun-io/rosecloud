package io.rosecloud.starter.security.token;

import io.jsonwebtoken.Claims;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.model.UserPrincipal;
import io.rosecloud.common.security.token.JwtPair;
import io.rosecloud.starter.security.config.SecurityProperties;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTokenFactoryTest {

    @Test
    void createTokenPairDefaultsSystemTenantWhenPrincipalHasNoTenant() {
        JwtTokenFactory factory = newFactory();
        SecurityUser user = new SecurityUser(1L, "admin", "Admin", null, true, null,
                new UserPrincipal(UserPrincipal.Type.USER_NAME, "admin"), List.of());

        JwtPair pair = factory.createTokenPair(user);

        assertEquals("ROOT", tenant(factory.parseAccessToken(pair.accessToken()).getPayload()));
        assertEquals("ROOT", tenant(factory.parseRefreshToken(pair.refreshToken()).getPayload()));
    }

    @Test
    void createTokenPairCanonicalizesExplicitTenantId() {
        JwtTokenFactory factory = newFactory();
        SecurityUser user = new SecurityUser(1L, "admin", "Admin", null, true, "tenant1",
                new UserPrincipal(UserPrincipal.Type.USER_NAME, "admin"), List.of());

        JwtPair pair = factory.createTokenPair(user, "tenant1");

        assertEquals("TENANT1", tenant(factory.parseAccessToken(pair.accessToken()).getPayload()));
        assertEquals("TENANT1", tenant(factory.parseRefreshToken(pair.refreshToken()).getPayload()));
    }

    private static JwtTokenFactory newFactory() {
        SecurityProperties properties = new SecurityProperties();
        properties.getJwt().setIssuer("rosecloud");
        properties.getJwt().setSecret(Base64.getEncoder().encodeToString(new byte[64]));
        properties.setAccessTokenExpirationSeconds(60);
        properties.setRefreshTokenExpirationSeconds(120);
        return new JwtTokenFactory(properties);
    }

    private static String tenant(Claims claims) {
        return claims.get("tenant", String.class);
    }
}
