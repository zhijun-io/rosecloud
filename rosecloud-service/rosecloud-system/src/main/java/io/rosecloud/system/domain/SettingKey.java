package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.HasKey;
import io.rosecloud.common.core.model.HasName;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Domain view of a configuration key definition.
 */
@Value
@AllArgsConstructor
public final class SettingKey implements HasKey, HasName {

    Long id;
    String key;
    String name;
    String remark;
    LocalDateTime createTime;
    Long createBy;
    LocalDateTime updateTime;
    Long updateBy;
}
