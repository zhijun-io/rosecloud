package io.rosecloud.starter.security;

import io.rosecloud.common.security.SecurityHeaders;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;
import io.rosecloud.starter.security.jwt.JwtProperties;
import io.rosecloud.starter.security.jwt.JwtTokenCodec;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityContextFilterTest {

    @Test
    void decodesIdentityFromBearerTokenAndIgnoresUserHeaders() throws Exception {
        JwtTokenCodec codec = jwtTokenCodec();
        SecurityContextFilter filter = new SecurityContextFilter(codec);

        CurrentUser tokenUser = new CurrentUser(42L, "token-user", 99L, List.of("tenant-admin"), null);
        String token = codec.issueAccessToken(tokenUser);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        request.addHeader(SecurityHeaders.TRACE_ID, "trace-123");

        AtomicReference<CurrentUser> seen = new AtomicReference<>();
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                seen.set(UserContext.get());
            }
        };

        filter.doFilter(request, new org.springframework.mock.web.MockHttpServletResponse(), chain);

        CurrentUser user = seen.get();
        assertThat(user).isNotNull();
        assertThat(user.userId()).isEqualTo(42L);
        assertThat(user.username()).isEqualTo("token-user");
        assertThat(user.tenantId()).isEqualTo(99L);
        assertThat(user.roles()).containsExactly("tenant-admin");
        assertThat(user.traceId()).isEqualTo("trace-123");
    }

    private static JwtTokenCodec jwtTokenCodec() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("01234567890123456789012345678901");
        properties.setIssuer("rosecloud-test");
        return new JwtTokenCodec(properties);
    }
}
