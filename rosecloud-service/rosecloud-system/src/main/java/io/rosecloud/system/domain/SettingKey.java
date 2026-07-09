package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.HasKey;
import io.rosecloud.common.core.model.HasName;

import java.time.LocalDateTime;

/**
 * Domain view of a configuration key definition.
 */
public final class SettingKey implements HasKey, HasName {
    private final Long id;
    private final String key;
    private final String name;
    private final String remark;
    private final LocalDateTime createTime;
    private final Long createBy;
    private final LocalDateTime updateTime;
    private final Long updateBy;

    public SettingKey(Long id, String key, String name, String remark) {
        this(id, key, name, remark, null, null, null, null);
    }

    public SettingKey(Long id, String key, String name, String remark,
                      LocalDateTime createTime, Long createBy,
                      LocalDateTime updateTime, Long updateBy) {
        this.id = id;
        this.key = key;
        this.name = name;
        this.remark = remark;
        this.createTime = createTime;
        this.createBy = createBy;
        this.updateTime = updateTime;
        this.updateBy = updateBy;
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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public Long getUpdateBy() {
        return updateBy;
    }
}
