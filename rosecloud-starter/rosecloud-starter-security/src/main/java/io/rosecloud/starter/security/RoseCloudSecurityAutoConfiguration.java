package io.rosecloud.starter.security;

import feign.RequestInterceptor;
import io.rosecloud.starter.security.config.SecurityConfiguration;
import io.rosecloud.starter.security.config.SecurityProperties;
import io.rosecloud.starter.security.feign.ServiceAuthRequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@EnableConfigurationProperties(SecurityProperties.class)
@Import(SecurityConfiguration.class)
public class RoseCloudSecurityAutoConfiguration {

    @Bean
    @ConditionalOnClass(name = {
            "feign.RequestInterceptor",
            "io.rosecloud.starter.tenant.core.TenantContextHolder"
    })
    public RequestInterceptor serviceAuthRequestInterceptor(Environment environment) {
        return new ServiceAuthRequestInterceptor(environment);
    }
}
