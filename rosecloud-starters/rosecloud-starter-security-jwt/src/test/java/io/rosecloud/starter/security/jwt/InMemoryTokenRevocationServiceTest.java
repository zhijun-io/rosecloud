package io.rosecloud.starter.security.jwt;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTokenRevocationServiceTest {

    @Test
    void revokedTokenIsReportedRevoked() {
        InMemoryTokenRevocationService service = new InMemoryTokenRevocationService();
        assertThat(service.isRevoked("jti-1")).isFalse();
        service.revoke("jti-1", Instant.now().plusSeconds(60));
        assertThat(service.isRevoked("jti-1")).isTrue();
    }

    @Test
    void expiredRevocationsEvictOnRead() {
        InMemoryTokenRevocationService service = new InMemoryTokenRevocationService();
        service.revoke("jti-2", Instant.now().minusSeconds(60));
        assertThat(service.isRevoked("jti-2")).isFalse();
    }

    @Test
    void nullAndBlankJtiAreNoOp() {
        InMemoryTokenRevocationService service = new InMemoryTokenRevocationService();
        service.revoke(null, Instant.now().plusSeconds(60));
        service.revoke("  ", Instant.now().plusSeconds(60));
        assertThat(service.isRevoked(null)).isFalse();
        assertThat(service.isRevoked("  ")).isFalse();
    }
}
