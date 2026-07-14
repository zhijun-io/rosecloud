package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasName;
import io.rosecloud.common.core.model.HasParentId;
import io.rosecloud.common.core.model.HasStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Value;

/** Domain view of a department/org node. ORM-free; mapped to/from {@code sys_dept}. */
@Value
public final class Dept implements HasId, HasName, HasParentId, HasStatus<Integer>, Serializable {

    Long id;
    Long parentId;
    String name;
    Integer sort;
    Integer status;
    String leader;
    String phone;
    LocalDateTime createTime;
    Long createBy;
    LocalDateTime updateTime;
    Long updateBy;

    public static Dept of(Long id, Long parentId, String name, Integer sort, Integer status, String leader,
                          String phone) {
        return new Dept(id, parentId, name, sort, status, leader, phone, null, null, null, null);
    }
}
