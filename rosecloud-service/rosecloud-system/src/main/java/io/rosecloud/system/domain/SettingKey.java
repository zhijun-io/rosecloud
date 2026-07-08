package io.rosecloud.system.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/** Domain view of a configuration key definition. */
public final class SettingKey {

    private final String key;
    private final String name;
    private final String remark;
    private final LocalDateTime updatedAt;
    private final Long updatedBy;

    public SettingKey(String key, String name, String remark, LocalDateTime updatedAt, Long updatedBy) {
        this.key = key;
        this.name = name;
        this.remark = remark;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public String getKey() { return key; }
    public String getName() { return name; }
    public String getRemark() { return remark; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getUpdatedBy() { return updatedBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SettingKey that)) return false;
        return Objects.equals(key, that.key) && Objects.equals(name, that.name)
                && Objects.equals(remark, that.remark) && Objects.equals(updatedAt, that.updatedAt)
                && Objects.equals(updatedBy, that.updatedBy);
    }

    @Override
    public int hashCode() { return Objects.hash(key, name, remark, updatedAt, updatedBy); }

    @Override
    public String toString() {
        return "SettingKey[" + "key=" + key + ", name=" + name + ", remark=" + remark
                + ", updatedAt=" + updatedAt + ", updatedBy=" + updatedBy + ']';
    }
}
