package io.rosecloud.starter.data;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import io.rosecloud.starter.data.cache.CaffeineEntityCache;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.CacheEvictionListener;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Registers the MyBatis-Plus interceptor chain and audit meta handler.
 *
 * <p>Inner-interceptor beans (e.g. a tenant line interceptor from
 * {@code rosecloud-starter-tenant}) are collected and added before pagination,
 * so cross-cutting SQL rewrites plug in without this starter depending on them.
 *
 * <p>Also wires the domain event publisher and cache eviction listener, providing
 * an event-driven caching layer similar to ThingsBoard's {@code AbstractCachedEntityService}.
 */
@AutoConfiguration
@ConditionalOnClass(MybatisPlusInterceptor.class)
public class RoseCloudMybatisPlusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor(List<InnerInterceptor> innerInterceptors) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        for (InnerInterceptor inner : innerInterceptors) {
            interceptor.addInnerInterceptor(inner);
        }
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(100L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    public AuditMetaObjectHandler auditMetaObjectHandler() {
        return new AuditMetaObjectHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public EntityEventPublisher entityEventPublisher(ApplicationEventPublisher publisher,
                                                     CacheEvictionListener cacheEvictionListener) {
        EntityEventPublisher ep = new EntityEventPublisher(publisher);
        ep.setCacheEvictionListener(cacheEvictionListener);
        return ep;
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheEvictionListener cacheEvictionListener(List<EntityCache<?, ?>> caches) {
        CacheEvictionListener listener = new CacheEvictionListener();
        listener.registerAll(caches);
        return listener;
    }
}
