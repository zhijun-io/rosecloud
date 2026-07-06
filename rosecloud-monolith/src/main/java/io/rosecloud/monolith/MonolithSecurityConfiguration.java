package io.rosecloud.monolith;

import io.rosecloud.starter.security.jwt.JwtTokenCodec;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

/**
 * Registers the monolith JWT filter ahead of the shared security-context filter
 * so identity headers are present before they are decoded.
 */
@Profile("monolith")
@Configuration
public class MonolithSecurityConfiguration {

    @Bean
    public FilterRegistrationBean<MonolithJwtFilter> monolithJwtFilterRegistration(JwtTokenCodec jwtTokenCodec) {
        FilterRegistrationBean<MonolithJwtFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new MonolithJwtFilter(jwtTokenCodec));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
