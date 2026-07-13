package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.common.core.model.ToData;
import io.rosecloud.common.core.model.ToEntity;
import io.rosecloud.starter.data.BaseEntity;
import io.rosecloud.system.domain.Dept;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent entity for {@code sys_dept}; confined to infrastructure. */
@TableName("sys_dept")
@Getter
@Setter
@NoArgsConstructor
public class DeptEntity extends BaseEntity implements ToData<Dept>, ToEntity<Dept, DeptEntity> {

    private Long parentId;
    private String name;
    private Integer sort;
    private Integer status;
    private String leader;
    private String phone;

    @Override
    public Dept toData() {
        return new Dept(getId(), parentId, name, sort, status, leader, phone, getCreateTime(),
                getCreateBy(), getUpdateTime(), getUpdateBy());
    }

    @Override
    public DeptEntity toEntity(Dept d) {
        setId(d.getId());
        setParentId(d.getParentId());
        setName(d.getName());
        setSort(d.getSort());
        setStatus(d.getStatus());
        setLeader(d.getLeader());
        setPhone(d.getPhone());
        setCreateTime(d.getCreateTime());
        setCreateBy(d.getCreateBy());
        setUpdateTime(d.getUpdateTime());
        setUpdateBy(d.getUpdateBy());
        return this;
    }
}
