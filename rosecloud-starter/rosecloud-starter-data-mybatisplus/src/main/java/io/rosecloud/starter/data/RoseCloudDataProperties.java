package io.rosecloud.starter.data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe binding for data-layer settings, replacing the ad-hoc {@code @Value}
 * placeholder previously inlined in {@code RoseCloudMybatisPlusAutoConfiguration}.
 */
@ConfigurationProperties(prefix = "rosecloud.data")
public class RoseCloudDataProperties {

    private String dbType = "MYSQL";

    /**
     * 分页查询的全局最大页大小。任何 {@code Page} 请求的 size 超过此值都会被
     * MyBatis-Plus 的 {@code PaginationInnerInterceptor} 在服务端截断，等价于原
     * {@code PageSupport.MAX_SIZE} 的防滥用量级，但改为声明式、对所有分页查询生效。
     */
    private Long maxPageSize = 100L;

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public Long getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(Long maxPageSize) {
        this.maxPageSize = maxPageSize;
    }
}
