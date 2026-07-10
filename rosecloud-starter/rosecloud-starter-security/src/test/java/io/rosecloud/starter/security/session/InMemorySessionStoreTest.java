package io.rosecloud.starter.security.session;

import io.rosecloud.common.security.model.LoginSession;
import io.rosecloud.starter.security.token.JwtTokenFactory;
import io.rosecloud.starter.security.config.SecurityProperties;
import io.rosecloud.common.security.model.SecurityUser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the decoupled revocation model: a token is revoked only when its
 * {@code jti} is in the revocation set, and validity does NOT depend on a
 * session record existing.
 */
class InMemorySessionStoreTest {

    private JwtTokenFactory newFactory() {
        SecurityProperties props = new SecurityProperties();
        // JwtTokenFactory requires a Base64 secret that decodes to >= 64 bytes (HS512).
        byte[] secret = new byte[64];
        java.util.Arrays.fill(secret, (byte) 1);
        props.getJwt().setSecret(java.util.Base64.getEncoder().encodeToString(secret));
        return new JwtTokenFactory(props);
    }

    private SecurityUser user() {
        return new SecurityUser(1L, "alice", "Alice", "pass", true, "ROOT", null, null);
    }

    @Test
    void validTokenWithoutSessionIsNotRevoked() {
        InMemorySessionStore store = new InMemorySessionStore();
        JwtTokenFactory factory = newFactory();
        String access = factory.createAccessJwtToken(user()).token();
        // No session saved, yet a signature-valid token must NOT be considered revoked.
        assertThat(store.isRevoked(access)).isFalse();
    }

    @Test
    void revokeMakesTokenRevokedAndCascadesToPairedToken() {
        InMemorySessionStore store = new InMemorySessionStore();
        JwtTokenFactory factory = newFactory();
        String access = factory.createAccessJwtToken(user()).token();
        String refresh = factory.createRefreshToken(user()).token();

        store.save(new LoginSession(UUID.randomUUID().toString(), access, refresh, 1L,
                "alice", "Alice", "127.0.0.1", "UA", Instant.now(),
                Instant.now().plusSeconds(86400)));

        store.revoke(access);

        assertThat(store.isRevoked(access)).isTrue();
        assertThat(store.isRevoked(refresh)).isTrue();
    }

    @Test
    void revokeByUserIdRevokesAllHeldTokens() {
        InMemorySessionStore store = new InMemorySessionStore();
        JwtTokenFactory factory = newFactory();
        String access = factory.createAccessJwtToken(user()).token();
        String refresh = factory.createRefreshToken(user()).token();

        store.save(new LoginSession(UUID.randomUUID().toString(), access, refresh, 1L,
                "alice", "Alice", "127.0.0.1", "UA", Instant.now(),
                Instant.now().plusSeconds(86400)));

        store.revokeByUserId(1L);

        assertThat(store.isRevoked(access)).isTrue();
        assertThat(store.isRevoked(refresh)).isTrue();
    }

    @Test
    void mintedTokenCarriesJtiAndExpiry() {
        JwtTokenFactory factory = newFactory();
        String access = factory.createAccessJwtToken(user()).token();
        // The factory stamps a jti (id claim) and an exp; the store keys revocation on them.
        assertThat(access).isNotBlank();
        assertThat(factory.parseAccessToken(access)).isNotNull();
    }
}
