package io.rosecloud.starter.data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe binding for data-layer settings, replacing the ad-hoc {@code @Value}
 * placeholder previously inlined in {@code RoseCloudMybatisPlusAutoConfiguration}.
 */
@ConfigurationProperties(prefix = "rosecloud.data")
public class RoseCloudDataProperties {

    private String dbType = "MYSQL";

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }
}
