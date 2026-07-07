package io.rosecloud.starter.tenant.mybatis;

import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import io.rosecloud.starter.tenant.core.TenantProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Registers the MyBatis-Plus {@link TenantLineInnerInterceptor} so row-level
 * tenant isolation is applied to SQL when {@code rosecloud.tenant.enabled=true}
 * and MyBatis-Plus is on the classpath. The interceptor is collected by
 * {@code rosecloud-starter-data-mybatisplus}'s {@code MybatisPlusInterceptor}
 * (which adds all {@code InnerInterceptor} beans before pagination).
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "rosecloud.tenant", name = "enabled", havingValue = "true")
@ConditionalOnClass(TenantLineInnerInterceptor.class)
public class TenantMybatisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TenantLineInnerInterceptor tenantLineInnerInterceptor(TenantProperties properties) {
        return new TenantLineInnerInterceptor(new RoseCloudTenantLineHandler(properties));
    }
}
