package io.rosecloud.starter.tenant;

import io.rosecloud.starter.tenant.async.TenantContextTaskDecorator;
import io.rosecloud.starter.tenant.core.TenantProperties;
import io.rosecloud.starter.tenant.resolver.HeaderTenantResolver;
import io.rosecloud.starter.tenant.resolver.TenantResolver;
import io.rosecloud.starter.tenant.web.TenantGatewayFilter;
import io.rosecloud.starter.tenant.web.TenantWebFilter;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Auto-configuration for multi-tenant support.
 *
 * <p>Activated by {@code rosecloud.tenant.enabled=true}; dormant otherwise. In
 * servlet apps it wires the resolver + web filter; in reactive (gateway) apps it
 * wires the gateway filter. Tenant context is propagated across {@code @Async}
 * boundaries via a {@link ThreadPoolTaskExecutor} task decorator.
 */
@AutoConfiguration
@EnableConfigurationProperties(TenantProperties.class)
@ConditionalOnProperty(prefix = "rosecloud.tenant", name = "enabled", havingValue = "true")
public class TenantAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantResolver.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public TenantResolver headerTenantResolver(TenantProperties properties) {
        return new HeaderTenantResolver(properties);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FilterRegistrationBean<TenantWebFilter> tenantWebFilter(TenantResolver resolver) {
        FilterRegistrationBean<TenantWebFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TenantWebFilter(resolver));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(TenantGatewayFilter.class)
    @ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public TenantGatewayFilter tenantGatewayFilter(TenantProperties properties) {
        return new TenantGatewayFilter(properties);
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
