package io.rosecloud.starter.security;

import feign.RequestInterceptor;
import io.rosecloud.api.user.SystemUserApi;
import io.rosecloud.starter.security.jwt.JwtTokenCodec;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Security-specific servlet wiring: JWT-backed caller identity plus outbound
 * Feign context propagation.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(InternalApiKeyProperties.class)
public class SecurityWebAutoConfiguration {

    @Bean
    public FilterRegistrationBean<SecurityContextFilter> securityContextFilterRegistration(JwtTokenCodec jwtTokenCodec,
                                                                                         SystemUserApi systemUserApi) {
        FilterRegistrationBean<SecurityContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SecurityContextFilter(jwtTokenCodec, systemUserApi));
        registration.addUrlPatterns("/*");
        // Must run AFTER the Spring Security filter chain (order -100) so the
        // SecurityContextPersistenceFilter does not reset the identity we set
        // here; method security then reads it when the controller is invoked.
        registration.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<InternalApiKeyFilter> internalApiKeyFilterRegistration(
            InternalApiKeyProperties properties) {
        FilterRegistrationBean<InternalApiKeyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new InternalApiKeyFilter(properties.getInternalApiKey()));
        registration.addUrlPatterns("/internal/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Bean
    @ConditionalOnClass(RequestInterceptor.class)
    public RequestInterceptor securityHeaderFeignPropagator(InternalApiKeyProperties properties) {
        return new SecurityHeaderFeignPropagator(properties.getInternalApiKey());
    }
}
