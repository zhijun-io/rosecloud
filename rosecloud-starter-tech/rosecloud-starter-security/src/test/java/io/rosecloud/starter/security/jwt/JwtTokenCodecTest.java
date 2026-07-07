package io.rosecloud.starter.security.jwt;

import io.rosecloud.common.security.context.CurrentUser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenCodecTest {

    @Test
    void issuesAndParsesJtiAndExpiry() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("01234567890123456789012345678901");
        JwtTokenCodec codec = new JwtTokenCodec(properties);

        CurrentUser user = new CurrentUser(1L, "alice", null, List.of("admin"));
        TokenClaims claims = codec.parse(codec.issueAccessToken(user));

        assertThat(claims.jti()).isNotBlank();
        assertThat(claims.expiresAt()).isAfter(Instant.now());
        assertThat(claims.username()).isEqualTo("alice");
        assertThat(claims.type()).isEqualTo(TokenType.ACCESS);
    }
}
