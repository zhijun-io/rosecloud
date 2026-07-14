package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasName;
import io.rosecloud.common.core.model.HasParentId;
import io.rosecloud.common.core.model.HasStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Value;

/** Domain view of a menu/permission node. ORM-free; mapped to/from {@code sys_menu}. */
@Value
@AllArgsConstructor
public final class Menu implements HasId, HasName, HasParentId, HasStatus<Integer>, Serializable {

    Long id;
    Long parentId;
    String name;
    Integer type;
    String path;
    String component;
    String perms;
    String icon;
    Integer sort;
    Integer status;
    Integer visible;
    LocalDateTime createTime;
    Long createBy;
    LocalDateTime updateTime;
    Long updateBy;

    public static Menu of(Long id, Long parentId, String name, Integer type, String path, String component,
                          String perms, String icon, Integer sort, Integer status, Integer visible) {
        return new Menu(id, parentId, name, type, path, component, perms, icon, sort, status, visible,
                null, null, null, null);
    }
}
