package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.common.core.model.ToData;
import io.rosecloud.common.core.model.ToEntity;
import io.rosecloud.starter.data.BaseEntity;
import io.rosecloud.system.domain.DictData;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent object for {@code sys_dict_data}; confined to infrastructure. */
@TableName("sys_dict_data")
@Getter
@Setter
@NoArgsConstructor
public class DictDataEntity extends BaseEntity implements ToData<DictData>, ToEntity<DictData, DictDataEntity> {

    private String dictCode;
    private String label;
    private String value;
    private Integer sort;
    private Integer status;
    private String remark;

    @Override
    public DictData toData() {
        return new DictData(getId(), dictCode, label, value, sort, status, remark, getCreateTime(),
                getCreateBy(), getUpdateTime(), getUpdateBy());
    }

    @Override
    public DictDataEntity toEntity(DictData d) {
        setId(d.getId());
        setDictCode(d.getDictCode());
        setLabel(d.getLabel());
        setValue(d.getValue());
        setSort(d.getSort());
        setStatus(d.getStatus());
        setRemark(d.getRemark());
        setCreateTime(d.getCreateTime());
        setCreateBy(d.getCreateBy());
        setUpdateTime(d.getUpdateTime());
        setUpdateBy(d.getUpdateBy());
        return this;
    }
}
