package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.common.core.model.ToData;
import io.rosecloud.common.core.model.ToEntity;
import io.rosecloud.starter.data.BaseEntity;
import io.rosecloud.system.domain.DictType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent object for {@code sys_dict_type}; confined to infrastructure. */
@TableName("sys_dict_type")
@Getter
@Setter
@NoArgsConstructor
public class DictTypeEntity extends BaseEntity implements ToData<DictType>, ToEntity<DictType, DictTypeEntity> {

    private String code;
    private String name;
    private Integer status;
    private String remark;

    @Override
    public DictType toData() {
        return new DictType(getId(), code, name, status, remark, getCreateTime(), getCreateBy(),
                getUpdateTime(), getUpdateBy());
    }

    @Override
    public DictTypeEntity toEntity(DictType d) {
        setId(d.getId());
        setCode(d.getCode());
        setName(d.getName());
        setStatus(d.getStatus());
        setRemark(d.getRemark());
        setCreateTime(d.getCreateTime());
        setCreateBy(d.getCreateBy());
        setUpdateTime(d.getUpdateTime());
        setUpdateBy(d.getUpdateBy());
        return this;
    }
}
