package io.rosecloud.starter.tenant;

import io.rosecloud.starter.tenant.async.TenantContextTaskDecorator;
import io.rosecloud.starter.tenant.core.TenantProperties;
import io.rosecloud.starter.tenant.web.TenantGatewayFilter;
import io.rosecloud.starter.tenant.web.TenantWebFilter;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Auto-configuration for multi-tenant support.
 *
 * <p>Activated by {@code rosecloud.tenant.enabled=true}; dormant otherwise. In
 * servlet apps it wires the web filter; in reactive (gateway) apps it wires the
 * gateway filter. Tenant context is propagated across {@code @Async} boundaries
 * via a {@link ThreadPoolTaskExecutor} task decorator.
 */
@AutoConfiguration
@EnableConfigurationProperties(TenantProperties.class)
@ConditionalOnProperty(prefix = "rosecloud.tenant", name = "enabled", havingValue = "true")
public class TenantAutoConfiguration {

    /** Default order of Spring Security's {@code FilterChainProxy} servlet registration. */
    private static final int SECURITY_FILTER_CHAIN_ORDER = -100;

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FilterRegistrationBean<TenantWebFilter> tenantWebFilter() {
        FilterRegistrationBean<TenantWebFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TenantWebFilter());
        registration.addUrlPatterns("/*");
        // Must run AFTER Spring Security's filter chain (default order -100) so the
        // authenticated principal is available; tenant is derived from it, never from a
        // client-supplied header.
        registration.setOrder(SECURITY_FILTER_CHAIN_ORDER + 10);
        return registration;
    }

    @Bean(name = "tenantGatewayFilter")
    @ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public TenantGatewayFilter tenantGatewayFilter() {
        return new TenantGatewayFilter();
    }

    @Bean
    public static BeanPostProcessor tenantTaskExecutorDecoratorPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof ThreadPoolTaskExecutor executor) {
                    executor.setTaskDecorator(new TenantContextTaskDecorator());
                }
                return bean;
            }
        };
    }
}
