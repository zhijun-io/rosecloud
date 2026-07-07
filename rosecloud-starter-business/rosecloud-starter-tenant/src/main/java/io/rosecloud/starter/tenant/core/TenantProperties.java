package io.rosecloud.starter.tenant.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

/** Configuration for {@code rosecloud.tenant.*}. */
@ConfigurationProperties(prefix = "rosecloud.tenant")
public class TenantProperties {

    /** Isolation strategy. COLUMN by default. */
    private MultiTenantType type = MultiTenantType.COLUMN;

    /** Tables ignored by row-level isolation. */
    private List<String> ignoreTables = Collections.emptyList();

    public MultiTenantType getType() {
        return type;
    }

    public void setType(MultiTenantType type) {
        this.type = type;
    }

    public List<String> getIgnoreTables() {
        return ignoreTables;
    }

    public void setIgnoreTables(List<String> ignoreTables) {
        this.ignoreTables = ignoreTables;
    }
}
