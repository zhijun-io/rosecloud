package io.rosecloud.system.config;

import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.starter.data.EntityCacheNames;
import io.rosecloud.starter.data.cache.CacheSpecs;
import io.rosecloud.starter.data.cache.CachingConfigProperties;
import io.rosecloud.starter.data.cache.CaffeineEntityCache;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.system.domain.Menu;
import io.rosecloud.system.domain.DictData;
import io.rosecloud.system.domain.DictType;
import io.rosecloud.system.domain.Dept;
import io.rosecloud.system.domain.Tenant;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;
import java.util.Optional;

/**
 * Handles system persistence mapper scanning and defines the local entity caches
 * used by the system service.
 *
 * <p>Each cache is backed by Caffeine (local heap). TTL and max size are configured
 * via {@code cache.specs.*} in {@code application.yml} (mirroring ThingsBoard's
 * {@code CacheSpecs} pattern), falling back to 5-minute TTL / 1000 max size.
 * Cache entries are evicted automatically when the corresponding
 * {@link EntityChangedEvent} is published and the transaction commits; see
 * {@link io.rosecloud.starter.data.event.CacheEvictionListener}.
 */
@Configuration
@MapperScan("io.rosecloud.system.persistence")
@EnableConfigurationProperties(UserActivationProperties.class)
@EnableScheduling
public class SystemCoreConfiguration {

    private final CachingConfigProperties cachingConfigProperties;

    public SystemCoreConfiguration(CachingConfigProperties cachingConfigProperties) {
        this.cachingConfigProperties = cachingConfigProperties;
    }

    /** 用户安全信息缓存（login → SecurityUser），查 5 张表后的结果。 */
    @Bean
    public EntityCache<String, SecurityUser> userSecurityCache() {
        return buildCache(EntityCacheNames.USER_SECURITY);
    }

    /** 用户权限 code 缓存（userId → perms）。 */
    @Bean
    public EntityCache<Long, List<String>> userPermsCache() {
        return buildCache(EntityCacheNames.USER_PERMS);
    }

    /** 角色菜单 ID 缓存（roleId → menuIds）。 */
    @Bean
    public EntityCache<Long, List<Long>> roleMenuIdsCache() {
        return buildCache(EntityCacheNames.ROLE_MENU_IDS);
    }

    /** 租户缓存（tenantId → Tenant）。 */
    @Bean
    public EntityCache<String, Tenant> tenantCache() {
        return buildCache(EntityCacheNames.TENANT);
    }

    /** 菜单缓存（menuId → Menu）。 */
    @Bean
    public EntityCache<Long, Menu> menuCache() {
        return buildCache(EntityCacheNames.MENU);
    }

    /** 菜单列表缓存（按角色 ID 集合查询的菜单列表，以及完整菜单树）。 */
    @Bean
    public EntityCache<String, List<Menu>> menuListCache() {
        return buildCache(EntityCacheNames.MENU_LIST);
    }

    /** 字典数据缓存（dictCode → dictionary items）。 */
    @Bean
    public EntityCache<String, List<DictData>> dictDataByCodeCache() {
        return buildCache(EntityCacheNames.DICT_DATA_BY_CODE);
    }

    /** 字典类型缓存（dictTypeId → DictType）。 */
    @Bean
    public EntityCache<Long, DictType> dictTypeCache() {
        return buildCache(EntityCacheNames.DICT_TYPE);
    }

    /** 部门缓存（deptId → Dept）。 */
    @Bean
    public EntityCache<Long, Dept> deptCache() {
        return buildCache(EntityCacheNames.DEPT);
    }

    /** 部门列表缓存（按不同查询条件的部门列表）。 */
    @Bean
    public EntityCache<String, List<Dept>> deptListCache() {
        return buildCache(EntityCacheNames.DEPT_LIST);
    }

    /**
     * Builds a {@link CaffeineEntityCache} using the configured {@link CacheSpecs}
     * for the given cache name, falling back to default specs if not explicitly configured.
     * Mirrors ThingsBoard's approach of externalizing cache TTL/maxSize.
     */
    private <K extends java.io.Serializable, V> CaffeineEntityCache<K, V> buildCache(String cacheName) {
        return new CaffeineEntityCache<>(cacheName, resolveSpecs(cacheName));
    }

    private CacheSpecs resolveSpecs(String cacheName) {
        return Optional.ofNullable(cachingConfigProperties.getSpecs())
                .map(specs -> specs.get(cacheName))
                .orElse(new CacheSpecs());
    }

}
