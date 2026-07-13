package io.rosecloud.starter.tenant;
import lombok.RequiredArgsConstructor;

import io.rosecloud.starter.tenant.async.TenantContextTaskDecorator;
import io.rosecloud.starter.tenant.core.MultiTenantType;
import io.rosecloud.starter.tenant.core.TenantProperties;
import io.rosecloud.starter.tenant.web.TenantWebFilter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Auto-configuration for multi-tenant support.
 *
 * <p>Multi-tenancy is enabled by default: the servlet web filter and async
 * decorators are always wired. The isolation {@link TenantProperties#getType()}
 * still governs behaviour — {@code COLUMN} applies row-level isolation, while
 * {@code NONE} makes the filter a no-op (no tenant predicate). In reactive
 * (gateway) apps the gateway filter strips any client-supplied tenant header.
 * Tenant context is propagated across {@code @Async} boundaries via a
 * {@link ThreadPoolTaskExecutor} task decorator.
 */
@RequiredArgsConstructor
@AutoConfiguration
@EnableConfigurationProperties(TenantProperties.class)
public class TenantAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TenantAutoConfiguration.class);

    private final TenantProperties tenantProperties;
    /** Default order of Spring Security's {@code FilterChainProxy} servlet registration. */
    private static final int SECURITY_FILTER_CHAIN_ORDER = -100;

    @PostConstruct
    public void validateTenantType() {
        MultiTenantType type = tenantProperties.getType();
        if (type == MultiTenantType.NONE) {
            log.info("Multi-tenant isolation is disabled (rosecloud.tenant.type=NONE); tenant filter is a no-op.");
        } else if (type != MultiTenantType.COLUMN) {
            log.warn("Tenant isolation type '{}' is not implemented; only COLUMN and NONE are supported. "
                    + "Falling back to row-level COLUMN isolation. Configure rosecloud.tenant.type=COLUMN.", type);
        }
    }

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
        registration.setAsyncSupported(true);
        return registration;
    }

    @Bean
    public static BeanPostProcessor tenantTaskExecutorDecoratorPostProcessor() {
        // Track executors we have already wrapped so we never stack the tenant decorator
        // on top of itself across multiple post-processor passes.
        java.util.Set<ThreadPoolTaskExecutor> wrapped = java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<>());
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof ThreadPoolTaskExecutor executor && wrapped.add(executor)) {
                    executor.setTaskDecorator(new TenantContextTaskDecorator());
                }
                return bean;
            }
        };
    }

    /**
     * Reactive (gateway) tenant wiring, isolated so the gateway {@code GlobalFilter}
     * class is only required when this configuration is actually activated. This
     * keeps {@link TenantAutoConfiguration} loadable in servlet-only deployments
     * (e.g. the monolith) that have no gateway on the classpath.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    static class ReactiveTenantConfiguration {

        @Bean(name = "tenantGatewayFilter")
        public io.rosecloud.starter.tenant.web.TenantGatewayFilter tenantGatewayFilter() {
            return new io.rosecloud.starter.tenant.web.TenantGatewayFilter();
        }
    }
}
