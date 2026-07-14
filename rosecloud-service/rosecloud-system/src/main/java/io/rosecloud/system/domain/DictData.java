package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Value;

/** Domain view of a dictionary item. ORM-free; mapped to/from {@code sys_dict_data}. */
@Value
public final class DictData implements HasId, HasStatus<Integer>, Serializable {

    Long id;
    String dictCode;
    String label;
    String value;
    Integer sort;
    Integer status;
    String remark;
    LocalDateTime createTime;
    Long createBy;
    LocalDateTime updateTime;
    Long updateBy;

    public static DictData of(Long id, String dictCode, String label, String value, Integer sort,
                              Integer status, String remark) {
        return new DictData(id, dictCode, label, value, sort, status, remark, null, null, null, null);
    }
}
