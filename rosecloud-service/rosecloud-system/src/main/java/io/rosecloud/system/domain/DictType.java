package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.HasCode;
import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasName;
import io.rosecloud.common.core.model.HasStatus;

import java.time.LocalDateTime;

import lombok.Value;

/**
 * Domain view of a dictionary type. ORM-free; mapped to/from {@code sys_dict_type}.
 */
@Value
public final class DictType implements HasId, HasCode, HasName, HasStatus<Integer> {

    Long id;
    String code;
    String name;
    Integer status;
    String remark;
    LocalDateTime createTime;
    Long createBy;
    LocalDateTime updateTime;
    Long updateBy;

    public static DictType of(Long id, String code, String name, Integer status, String remark) {
        return new DictType(id, code, name, status, remark, null, null, null, null);
    }
}
