package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasName;
import io.rosecloud.common.core.model.HasParentId;
import io.rosecloud.common.core.model.HasStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/** Domain view of a department/org node. ORM-free; mapped to/from {@code sys_dept}. */
public final class Dept implements HasId, HasName, HasParentId, HasStatus<Integer> {

    private final Long id;
    private final Long parentId;
    private final String name;
    private final Integer sort;
    private final Integer status;
    private final String leader;
    private final String phone;
    private final LocalDateTime createTime;
    private final Long createBy;
    private final LocalDateTime updateTime;
    private final Long updateBy;

    public Dept(Long id, Long parentId, String name, Integer sort, Integer status, String leader, String phone) {
        this(id, parentId, name, sort, status, leader, phone, null, null, null, null);
    }

    public Dept(Long id, Long parentId, String name, Integer sort, Integer status, String leader, String phone,
                LocalDateTime createTime, Long createBy, LocalDateTime updateTime, Long updateBy) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.sort = sort;
        this.status = status;
        this.leader = leader;
        this.phone = phone;
        this.createTime = createTime;
        this.createBy = createBy;
        this.updateTime = updateTime;
        this.updateBy = updateBy;
    }

    public Long getId() { return id; }
    public Long getParentId() { return parentId; }
    public String getName() { return name; }
    public Integer getSort() { return sort; }
    public Integer getStatus() { return status; }
    public String getLeader() { return leader; }
    public String getPhone() { return phone; }
    public LocalDateTime getCreateTime() { return createTime; }
    public Long getCreateBy() { return createBy; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public Long getUpdateBy() { return updateBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dept dept)) return false;
        return Objects.equals(id, dept.id) && Objects.equals(parentId, dept.parentId) && Objects.equals(name, dept.name)
                && Objects.equals(sort, dept.sort) && Objects.equals(status, dept.status)
                && Objects.equals(leader, dept.leader) && Objects.equals(phone, dept.phone)
                && Objects.equals(createTime, dept.createTime) && Objects.equals(createBy, dept.createBy)
                && Objects.equals(updateTime, dept.updateTime) && Objects.equals(updateBy, dept.updateBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, parentId, name, sort, status, leader, phone, createTime, createBy, updateTime, updateBy);
    }

    @Override
    public String toString() {
        return "Dept[" +
                "id=" + id +
                ", parentId=" + parentId +
                ", name=" + name +
                ", sort=" + sort +
                ", status=" + status +
                ", leader=" + leader +
                ", phone=" + phone +
                ", createTime=" + createTime +
                ", createBy=" + createBy +
                ", updateTime=" + updateTime +
                ", updateBy=" + updateBy +
                ']';
    }
}
