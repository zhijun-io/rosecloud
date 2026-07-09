package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.HasCode;
import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasName;

import java.time.LocalDateTime;
import java.util.Objects;

/** Domain view of a role. ORM-free; the persistence layer maps to/from {@code sys_role}. */
public final class Role implements HasId, HasCode, HasName {

    private final Long id;
    private final String code;
    private final String name;
    private final LocalDateTime createTime;
    private final Long createBy;
    private final LocalDateTime updateTime;
    private final Long updateBy;

    public Role(Long id, String code, String name) {
        this(id, code, name, null, null, null, null);
    }

    public Role(Long id, String code, String name, LocalDateTime createTime, Long createBy,
                LocalDateTime updateTime, Long updateBy) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.createTime = createTime;
        this.createBy = createBy;
        this.updateTime = updateTime;
        this.updateBy = updateBy;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public LocalDateTime getCreateTime() { return createTime; }
    public Long getCreateBy() { return createBy; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public Long getUpdateBy() { return updateBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        return Objects.equals(id, role.id) && Objects.equals(code, role.code) && Objects.equals(name, role.name)
                && Objects.equals(createTime, role.createTime) && Objects.equals(createBy, role.createBy)
                && Objects.equals(updateTime, role.updateTime) && Objects.equals(updateBy, role.updateBy);
    }

    @Override
    public int hashCode() { return Objects.hash(id, code, name, createTime, createBy, updateTime, updateBy); }

    @Override
    public String toString() {
        return "Role[" + "id=" + id + ", code=" + code + ", name=" + name +
                ", createTime=" + createTime + ", createBy=" + createBy +
                ", updateTime=" + updateTime + ", updateBy=" + updateBy + ']';
    }
}
