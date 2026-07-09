package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.HasKey;
import io.rosecloud.common.core.model.HasName;
import io.rosecloud.common.core.model.HasUpdatedAt;
import io.rosecloud.common.core.model.HasUpdatedBy;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain view of a configuration key definition.
 */
public final class SettingKey implements HasKey, HasName {
    private final Long id;
    private final String key;
    private final String name;
    private final String remark;

    public SettingKey(Long id, String key, String name, String remark) {
        this.id = id;
        this.key = key;
        this.name = name;
        this.remark = remark;
    }

    public Long getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getRemark() {
        return remark;
    }
}
