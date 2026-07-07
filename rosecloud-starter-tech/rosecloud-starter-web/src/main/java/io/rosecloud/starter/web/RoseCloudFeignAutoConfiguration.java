package io.rosecloud.starter.web;

import io.rosecloud.starter.web.security.SecurityHeaderFeignPropagator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Propagates {@code SecurityHeaders} onto outbound Feign requests. Only active
 * when OpenFeign is on the classpath.
 */
@AutoConfiguration
@ConditionalOnClass(name = "feign.RequestInterceptor")
public class RoseCloudFeignAutoConfiguration {

    @Bean
    public SecurityHeaderFeignPropagator securityHeaderFeignPropagator() {
        return new SecurityHeaderFeignPropagator();
    }
}
