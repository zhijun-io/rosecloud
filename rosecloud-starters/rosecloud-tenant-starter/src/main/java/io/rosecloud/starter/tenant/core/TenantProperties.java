package io.rosecloud.starter.tenant.core;

import io.rosecloud.common.security.SecurityHeaders;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

/** Configuration for {@code rosecloud.tenant.*}. */
@ConfigurationProperties(prefix = "rosecloud.tenant")
public class TenantProperties {

    /** Isolation strategy. COLUMN by default. */
    private MultiTenantType type = MultiTenantType.COLUMN;

    /** How the tenant id is resolved on inbound requests: {@code header} by default. */
    private String resolver = "header";

    /** Header name carrying the tenant id; defaults to {@link SecurityHeaders#TENANT_ID}. */
    private String headerName = SecurityHeaders.TENANT_ID;

    /** Tables ignored by row-level isolation. */
    private List<String> ignoreTables = Collections.emptyList();

    public MultiTenantType getType() {
        return type;
    }

    public void setType(MultiTenantType type) {
        this.type = type;
    }

    public String getResolver() {
        return resolver;
    }

    public void setResolver(String resolver) {
        this.resolver = resolver;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public List<String> getIgnoreTables() {
        return ignoreTables;
    }

    public void setIgnoreTables(List<String> ignoreTables) {
        this.ignoreTables = ignoreTables;
    }
}
