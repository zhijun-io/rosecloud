package io.rosecloud.starter.tenant.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import io.rosecloud.starter.tenant.core.TenantProperties;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;

/**
 * MyBatis-Plus tenant-line handler backed by {@link TenantContextHolder}. Row-level
 * isolation applies only when a tenant is bound for the current request;
 * platform/system traffic (no tenant) bypasses rewriting and sees all rows.
 * Tables without a {@code tenant_id} column must be listed in
 * {@code rosecloud.tenant.ignore-tables}.
 */
public class RoseCloudTenantLineHandler implements TenantLineHandler {

    private final TenantProperties properties;

    public RoseCloudTenantLineHandler(TenantProperties properties) {
        this.properties = properties;
    }

    @Override
    public Expression getTenantId() {
        String tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? null : new StringValue(tenantId);
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if (!TenantContextHolder.hasTenant()) {
            return true;
        }
        return properties.getIgnoreTables().contains(tableName);
    }
}
