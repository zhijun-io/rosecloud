package io.rosecloud.starter.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that {@code @PreAuthorize} is enforced by the Spring Security
 * method-security advisor in this module's context. Run with
 * {@code webEnvironment = NONE} so the servlet-only auto-configurations
 * (including {@code SecurityWebAutoConfiguration}) do not activate; the servlet
 * {@code SecurityContextFilter} is what populates the {@code SecurityContextHolder}
 * (covered by {@link SecurityContextFilterTest}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = MethodSecurityWiringTest.Config.class)
class MethodSecurityWiringTest {

    @Autowired
    SecuredService service;

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean
        SecuredService securedService() {
            return new SecuredService();
        }
    }

    static class SecuredService {
        @PreAuthorize("hasAuthority('platform-admin')")
        String admin() {
            return "ok";
        }

        @PreAuthorize("isAuthenticated()")
        String me() {
            return "ok";
        }
    }

    @Test
    @WithMockUser(authorities = "platform-admin")
    void adminAuthorityGrantsAdminMethod() {
        assertThat(service.admin()).isEqualTo("ok");
    }

    @Test
    @WithMockUser(authorities = "tenant-user")
    void nonAdminAuthorityIsDeniedOnAdminMethod() {
        assertThrows(AccessDeniedException.class, () -> service.admin());
    }

    @Test
    @WithMockUser
    void authenticatedUserGrantsSelfMethod() {
        assertThat(service.me()).isEqualTo("ok");
    }

    @Test
    @WithAnonymousUser
    void anonymousIsDeniedOnSelfMethod() {
        assertThrows(AccessDeniedException.class, () -> service.me());
    }
}
