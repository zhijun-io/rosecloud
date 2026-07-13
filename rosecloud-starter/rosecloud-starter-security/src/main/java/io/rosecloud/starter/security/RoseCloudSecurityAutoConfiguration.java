package io.rosecloud.starter.security;

import feign.RequestInterceptor;
import io.rosecloud.common.security.credential.PasswordPolicyValidator;
import io.rosecloud.starter.security.config.SecurityConfiguration;
import io.rosecloud.starter.security.config.PasswordPolicyProperties;
import io.rosecloud.starter.security.config.SecurityProperties;
import io.rosecloud.starter.security.feign.ServiceAuthRequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@EnableConfigurationProperties({SecurityProperties.class, PasswordPolicyProperties.class})
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

    /**
     * Creates the shared password policy validator from properties. Modules that
     * accept password writes (auth, system) inject this bean.  ThingsBoard
     * registers a similar validator from its {@code SecuritySettings} model.
     */
    @Bean
    @ConditionalOnMissingBean
    public PasswordPolicyValidator passwordPolicyValidator(PasswordPolicyProperties properties) {
        return new PasswordPolicyValidator(properties.toPolicy());
    }
}
