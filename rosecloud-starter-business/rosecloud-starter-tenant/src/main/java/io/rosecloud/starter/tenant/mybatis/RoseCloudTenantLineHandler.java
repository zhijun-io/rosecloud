package io.rosecloud.starter.tenant.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import io.rosecloud.starter.tenant.core.TenantContext;
import io.rosecloud.starter.tenant.core.TenantProperties;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

/**
 * MyBatis-Plus tenant-line handler backed by {@link TenantContext}. Row-level
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
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? null : new LongValue(tenantId);
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if (!TenantContext.hasTenant()) {
            return true;
        }
        return properties.getIgnoreTables().contains(tableName);
    }
}
