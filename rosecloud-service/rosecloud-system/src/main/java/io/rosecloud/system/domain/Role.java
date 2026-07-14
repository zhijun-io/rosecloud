package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.HasCode;
import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasName;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Value;

/** Domain view of a role. ORM-free; the persistence layer maps to/from {@code sys_role}. */
@Value
@AllArgsConstructor
public final class Role implements HasId, HasCode, HasName, Serializable {

    Long id;
    String code;
    String name;
    LocalDateTime createTime;
    Long createBy;
    LocalDateTime updateTime;
    Long updateBy;
}
