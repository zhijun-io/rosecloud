package io.rosecloud.starter.data.dynamic;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration for {@code rosecloud.datasource.dynamic.*}. When
 * {@code enabled=true}, the data starter builds a routing datasource over the
 * configured targets and makes it the primary {@code DataSource}; otherwise the
 * single-datasource path is untouched.
 *
 * <pre>
 * rosecloud:
 *   datasource:
 *     dynamic:
 *       enabled: true
 *       primary: primary
 *       datasources:
 *         primary:  { url: jdbc:mysql://..., username: ..., password: ... }
 *         business: { url: jdbc:mysql://..., username: ..., password: ... }
 * </pre>
 */
@ConfigurationProperties(prefix = "rosecloud.datasource.dynamic")
public class DynamicDataSourceProperties {

    private boolean enabled = false;

    /** Key of the default/fallback datasource; must exist in {@link #datasources}. */
    private String primary = "primary";

    private Map<String, DataSourceDefinition> datasources = new LinkedHashMap<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getPrimary() { return primary; }
    public void setPrimary(String primary) { this.primary = primary; }
    public Map<String, DataSourceDefinition> getDatasources() { return datasources; }
    public void setDatasources(Map<String, DataSourceDefinition> datasources) {
        this.datasources = datasources;
    }

    /** Connection settings for one target datasource. */
    public static class DataSourceDefinition {

        private String url;
        private String username;
        private String password;
        private String driverClassName;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDriverClassName() { return driverClassName; }
        public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
    }
}
