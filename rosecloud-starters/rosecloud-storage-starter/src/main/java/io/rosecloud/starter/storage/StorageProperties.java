package io.rosecloud.starter.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for {@code rosecloud.storage.*}. */
@ConfigurationProperties(prefix = "rosecloud.storage")
public class StorageProperties {

    /** Enable the file storage starter. */
    private boolean enabled = false;

    /** Backend type: {@code local} (default, filesystem) or a custom type whose bean the consumer provides (e.g. s3). */
    private String type = "local";

    /** Base directory for the local backend. */
    private String baseDir = "./data/storage";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }
}
