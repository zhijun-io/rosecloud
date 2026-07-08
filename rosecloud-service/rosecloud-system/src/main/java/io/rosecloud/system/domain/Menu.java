package io.rosecloud.system.domain;

import java.util.Objects;

/** Domain view of a menu/permission node. ORM-free; mapped to/from {@code sys_menu}. */
public final class Menu {

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

    public Menu(Long id, Long parentId, String name, Integer type, String path, String component, String perms,
                String icon, Integer sort, Integer status, Integer visible) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Menu menu)) return false;
        return Objects.equals(id, menu.id) && Objects.equals(parentId, menu.parentId) && Objects.equals(name, menu.name)
                && Objects.equals(type, menu.type) && Objects.equals(path, menu.path)
                && Objects.equals(component, menu.component) && Objects.equals(perms, menu.perms)
                && Objects.equals(icon, menu.icon) && Objects.equals(sort, menu.sort)
                && Objects.equals(status, menu.status) && Objects.equals(visible, menu.visible);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, parentId, name, type, path, component, perms, icon, sort, status, visible);
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
                ']';
    }
}
