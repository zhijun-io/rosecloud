package io.rosecloud.starter.security;

import feign.RequestInterceptor;
import io.rosecloud.api.user.SystemUserApi;
import io.rosecloud.starter.security.jwt.JwtTokenCodec;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Security-specific servlet wiring: JWT-backed caller identity plus outbound
 * Feign context propagation.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityWebAutoConfiguration {

    @Bean
    public FilterRegistrationBean<SecurityContextFilter> securityContextFilterRegistration(JwtTokenCodec jwtTokenCodec,
                                                                                         SystemUserApi systemUserApi) {
        FilterRegistrationBean<SecurityContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SecurityContextFilter(jwtTokenCodec, systemUserApi));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }

    @Bean
    @ConditionalOnClass(RequestInterceptor.class)
    public RequestInterceptor securityHeaderFeignPropagator() {
        return new SecurityHeaderFeignPropagator();
    }
}
