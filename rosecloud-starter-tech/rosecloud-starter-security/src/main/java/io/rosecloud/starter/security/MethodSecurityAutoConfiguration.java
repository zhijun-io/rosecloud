package io.rosecloud.starter.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Enables Spring Security method security ({@code @PreAuthorize}) for servlet
 * modules. The servlet {@link SecurityContextFilter} populates the
 * {@code SecurityContextHolder} from the bearer JWT, so endpoint-level
 * {@code @PreAuthorize} rules are enforced by the method-security advisor.
 *
 * <p>The HTTP layer is left permissive on purpose: authentication is verified by
 * the gateway / {@code MonolithJwtFilter} / {@link SecurityContextFilter}, and
 * authorization is decided per-endpoint via {@code @PreAuthorize}. A
 * {@code SecurityFilterChain} bean is still required so Spring Security does not
 * install its default deny-all chain; it is registered only when no other chain
 * exists and OAuth2 resource-server mode is disabled.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableMethodSecurity
public class MethodSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    @ConditionalOnProperty(prefix = "rosecloud.oauth2", name = "enabled", havingValue = "false", matchIfMissing = true)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(reg -> reg.anyRequest().permitAll());
        return http.build();
    }
}
