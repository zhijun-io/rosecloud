package io.rosecloud.starter.data.dynamic;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the dynamic (routing) datasource when
 * {@code rosecloud.datasource.dynamic.enabled=true}. Dormant otherwise, leaving
 * the single-datasource auto-configuration untouched. The routing datasource is
 * {@link Primary}, so MyBatis-Plus and transaction management bind to it; a
 * {@link DataSourceRoute} bean (defaulted here to "always primary") is the
 * extension point for tenant/business data routing.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "rosecloud.datasource.dynamic", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(DynamicDataSourceProperties.class)
public class DynamicDataSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataSourceRoute defaultDataSourceRoute() {
        return () -> null;
    }

    @Bean
    @Primary
    public DataSource dynamicDataSource(DynamicDataSourceProperties properties, DataSourceRoute route) {
        Map<String, DataSource> targets = new LinkedHashMap<>();
        properties.getDatasources().forEach((name, def) -> targets.put(name, buildDataSource(def)));
        if (!targets.containsKey(properties.getPrimary())) {
            throw new IllegalStateException(
                    "dynamic datasource primary '" + properties.getPrimary() + "' is not configured");
        }
        return new RoseCloudRoutingDataSource(targets, properties.getPrimary(), route);
    }

    private static DataSource buildDataSource(DynamicDataSourceProperties.DataSourceDefinition def) {
        DataSourceBuilder<?> builder = DataSourceBuilder.create();
        if (def.getUrl() != null) {
            builder.url(def.getUrl());
        }
        if (def.getUsername() != null) {
            builder.username(def.getUsername());
        }
        if (def.getPassword() != null) {
            builder.password(def.getPassword());
        }
        if (def.getDriverClassName() != null) {
            builder.driverClassName(def.getDriverClassName());
        }
        return builder.build();
    }
}
