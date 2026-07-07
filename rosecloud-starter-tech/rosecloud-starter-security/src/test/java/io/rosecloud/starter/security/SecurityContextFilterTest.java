package io.rosecloud.starter.security;

import io.rosecloud.api.user.SystemUserApi;
import io.rosecloud.api.user.UserAuthInfo;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;
import io.rosecloud.starter.security.jwt.JwtProperties;
import io.rosecloud.starter.security.jwt.JwtTokenCodec;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityContextFilterTest {

    @AfterEach
    void clearContext() {
        UserContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void decodesIdentityFromBearerTokenAndPopulatesSecurityContext() throws Exception {
        JwtTokenCodec codec = jwtTokenCodec();
        SecurityContextFilter filter = new SecurityContextFilter(codec, new SystemUserApi() {
            @Override
            public ApiResponse<UserAuthInfo> getAuthInfo(String username) {
                // getAuthInfo returns its own perms, but the filter must use the
                // perms embedded in the JWT (see tokenUser below), not these.
                return ApiResponse.ok(new UserAuthInfo(42L, username, "ignored", 1, 99L,
                        List.of("tenant-admin"), List.of("system:role:perm")));
            }
        });

        CurrentUser tokenUser = new CurrentUser(42L, "token-user", 99L,
                List.of("tenant-admin"), List.of("system:user:add", "system:user:edit"));
        String token = codec.issueAccessToken(tokenUser);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        AtomicReference<CurrentUser> seenUser = new AtomicReference<>();
        AtomicReference<String> seenToken = new AtomicReference<>();
        AtomicReference<org.springframework.security.core.Authentication> seenAuth = new AtomicReference<>();
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                seenUser.set(UserContext.get());
                seenToken.set(UserContext.getToken());
                seenAuth.set(SecurityContextHolder.getContext().getAuthentication());
            }
        };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        CurrentUser user = seenUser.get();
        assertThat(user).isNotNull();
        assertThat(user.userId()).isEqualTo(42L);
        assertThat(user.username()).isEqualTo("token-user");
        assertThat(user.tenantId()).isEqualTo(99L);
        assertThat(user.roles()).containsExactly("tenant-admin");
        // Perms come from the JWT, not from getAuthInfo (which returned system:role:perm).
        assertThat(user.perms()).containsExactly("system:user:add", "system:user:edit");

        assertThat(seenToken.get()).isEqualTo(token);

        assertThat(seenAuth.get()).isNotNull();
        assertThat(seenAuth.get().isAuthenticated()).isTrue();
        assertThat(seenAuth.get().getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("tenant-admin", "system:user:add", "system:user:edit");

        // Context is cleared after the request completes.
        assertThat(UserContext.get()).isNull();
        assertThat(UserContext.getToken()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static JwtTokenCodec jwtTokenCodec() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("01234567890123456789012345678901");
        properties.setIssuer("rosecloud-test");
        return new JwtTokenCodec(properties);
    }
}
