package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasName;
import io.rosecloud.common.core.model.HasParentId;
import io.rosecloud.common.core.model.HasStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/** Domain view of a menu/permission node. ORM-free; mapped to/from {@code sys_menu}. */
public final class Menu implements HasId, HasName, HasParentId, HasStatus<Integer> {

    private final Long id;
    private final Long parentId;
    private final String name;
    private final Integer type;
    private final String path;
    private final String component;
    private final String perms;
    private final String icon;
    private final Integer sort;
    private final Integer status;
    private final Integer visible;
    private final LocalDateTime createTime;
    private final Long createBy;
    private final LocalDateTime updateTime;
    private final Long updateBy;

    public Menu(Long id, Long parentId, String name, Integer type, String path, String component, String perms,
                String icon, Integer sort, Integer status, Integer visible) {
        this(id, parentId, name, type, path, component, perms, icon, sort, status, visible,
                null, null, null, null);
    }

    public Menu(Long id, Long parentId, String name, Integer type, String path, String component, String perms,
                String icon, Integer sort, Integer status, Integer visible, LocalDateTime createTime,
                Long createBy, LocalDateTime updateTime, Long updateBy) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.type = type;
        this.path = path;
        this.component = component;
        this.perms = perms;
        this.icon = icon;
        this.sort = sort;
        this.status = status;
        this.visible = visible;
        this.createTime = createTime;
        this.createBy = createBy;
        this.updateTime = updateTime;
        this.updateBy = updateBy;
    }

    public Long getId() { return id; }
    public Long getParentId() { return parentId; }
    public String getName() { return name; }
    public Integer getType() { return type; }
    public String getPath() { return path; }
    public String getComponent() { return component; }
    public String getPerms() { return perms; }
    public String getIcon() { return icon; }
    public Integer getSort() { return sort; }
    public Integer getStatus() { return status; }
    public Integer getVisible() { return visible; }
    public LocalDateTime getCreateTime() { return createTime; }
    public Long getCreateBy() { return createBy; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public Long getUpdateBy() { return updateBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Menu menu)) return false;
        return Objects.equals(id, menu.id) && Objects.equals(parentId, menu.parentId) && Objects.equals(name, menu.name)
                && Objects.equals(type, menu.type) && Objects.equals(path, menu.path)
                && Objects.equals(component, menu.component) && Objects.equals(perms, menu.perms)
                && Objects.equals(icon, menu.icon) && Objects.equals(sort, menu.sort)
                && Objects.equals(status, menu.status) && Objects.equals(visible, menu.visible)
                && Objects.equals(createTime, menu.createTime) && Objects.equals(createBy, menu.createBy)
                && Objects.equals(updateTime, menu.updateTime) && Objects.equals(updateBy, menu.updateBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, parentId, name, type, path, component, perms, icon, sort, status, visible,
                createTime, createBy, updateTime, updateBy);
    }

    @Override
    public String toString() {
        return "Menu[" +
                "id=" + id +
                ", parentId=" + parentId +
                ", name=" + name +
                ", type=" + type +
                ", path=" + path +
                ", component=" + component +
                ", perms=" + perms +
                ", icon=" + icon +
                ", sort=" + sort +
                ", status=" + status +
                ", visible=" + visible +
                ", createTime=" + createTime +
                ", createBy=" + createBy +
                ", updateTime=" + updateTime +
                ", updateBy=" + updateBy +
                ']';
    }
}
