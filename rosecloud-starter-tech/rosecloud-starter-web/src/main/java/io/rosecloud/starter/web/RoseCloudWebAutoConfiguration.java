package io.rosecloud.starter.web;

import io.rosecloud.starter.web.exception.GlobalExceptionHandler;
import io.rosecloud.starter.web.security.SecurityContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Cross-cutting web beans for servlet services: a global exception handler and
 * the security-context filter that decodes {@code SecurityHeaders} into
 * {@link io.rosecloud.common.security.context.UserContext}.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RoseCloudWebAutoConfiguration {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public FilterRegistrationBean<SecurityContextFilter> securityContextFilterRegistration() {
        FilterRegistrationBean<SecurityContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SecurityContextFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }
}
