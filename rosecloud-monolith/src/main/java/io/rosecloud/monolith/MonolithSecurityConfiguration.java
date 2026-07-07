package io.rosecloud.monolith;

import io.rosecloud.starter.security.jwt.JwtTokenCodec;
import io.rosecloud.starter.security.jwt.TokenRevocationService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Monolith security wiring (active under the {@code monolith} profile). With no
 * gateway in front, {@link MonolithJwtFilter} verifies HS256 JWTs and injects
 * identity headers ahead of the shared {@code SecurityContextFilter}.
 *
 * <p>Because the OAuth2 starter puts Spring Security on the classpath, Spring
 * Boot would otherwise auto-configure a default {@link SecurityFilterChain} that
 * blocks every route (including login). When OAuth2 is disabled (the default),
 * a permissive chain is registered so the default one does not activate; the
 * {@link MonolithJwtFilter} performs the real enforcement. Enable
 * {@code rosecloud.oauth2} to use the OAuth2 resource-server chain instead.
 */
@Profile("monolith")
@Configuration
@EnableConfigurationProperties(MonolithSecurityProperties.class)
public class MonolithSecurityConfiguration {

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "rosecloud.oauth2", name = "enabled", havingValue = "false", matchIfMissing = true)
    public SecurityFilterChain monolithSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(reg -> reg.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public FilterRegistrationBean<MonolithJwtFilter> monolithJwtFilterRegistration(
            JwtTokenCodec jwtTokenCodec, TokenRevocationService tokenRevocationService,
            MonolithSecurityProperties properties) {
        FilterRegistrationBean<MonolithJwtFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new MonolithJwtFilter(jwtTokenCodec, tokenRevocationService, properties));
        registration.addUrlPatterns("/*");
        // Run before the shared SecurityContextFilter (HIGHEST_PRECEDENCE + 20) so the
        // injected identity headers are present before they are decoded.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
