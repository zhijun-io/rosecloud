package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasName;
import io.rosecloud.common.core.model.HasParentId;
import io.rosecloud.common.core.model.HasStatus;

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

    public Dept(Long id, Long parentId, String name, Integer sort, Integer status, String leader, String phone) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.sort = sort;
        this.status = status;
        this.leader = leader;
        this.phone = phone;
    }

    public Long getId() { return id; }
    public Long getParentId() { return parentId; }
    public String getName() { return name; }
    public Integer getSort() { return sort; }
    public Integer getStatus() { return status; }
    public String getLeader() { return leader; }
    public String getPhone() { return phone; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dept dept)) return false;
        return Objects.equals(id, dept.id) && Objects.equals(parentId, dept.parentId) && Objects.equals(name, dept.name)
                && Objects.equals(sort, dept.sort) && Objects.equals(status, dept.status)
                && Objects.equals(leader, dept.leader) && Objects.equals(phone, dept.phone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, parentId, name, sort, status, leader, phone);
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
                ']';
    }
}
