package io.rosecloud.starter.security.oauth2;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Auto-configuration for an OAuth2 JWT resource server.
 *
 * <p>Activated by {@code rosecloud.oauth2.enabled=true} in a servlet app. Configures
 * a {@link SecurityFilterChain} that requires authentication on all requests and
 * validates bearer JWTs against the configured JWK set / issuer. Override the
 * {@code SecurityFilterChain} bean to customise authorization rules.
 */
@AutoConfiguration
@EnableConfigurationProperties(OAuth2Properties.class)
@ConditionalOnProperty(prefix = "rosecloud.oauth2", name = "enabled", havingValue = "true")
@ConditionalOnClass(name = "org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class OAuth2ResourceServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http, OAuth2Properties properties) throws Exception {
        http.authorizeHttpRequests(reg -> reg.anyRequest().authenticated())
                .oauth2ResourceServer(server -> server.jwt(jwt -> {
                    if (properties.getJwkSetUri() != null) {
                        jwt.jwkSetUri(properties.getJwkSetUri());
                    }
                }));
        return http.build();
    }
}
