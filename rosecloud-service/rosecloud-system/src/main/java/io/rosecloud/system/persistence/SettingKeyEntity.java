package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.common.core.model.ToData;
import io.rosecloud.common.core.model.ToEntity;
import io.rosecloud.starter.data.BaseEntity;
import io.rosecloud.system.domain.SettingKey;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@TableName("sys_setting_key")
@Getter
@Setter
@NoArgsConstructor
public class SettingKeyEntity extends BaseEntity implements ToData<SettingKey>, ToEntity<SettingKey, SettingKeyEntity> {

    private String key;
    private String name;
    private String remark;

    @Override
    public SettingKey toData() {
        return new SettingKey(getId(), key, name, remark, getCreateTime(), getCreateBy(),
                getUpdateTime(), getUpdateBy());
    }

    @Override
    public SettingKeyEntity toEntity(SettingKey settingKey) {
        setId(settingKey.getId());
        setKey(settingKey.getKey());
        setName(settingKey.getName());
        setRemark(settingKey.getRemark());
        setCreateTime(settingKey.getCreateTime());
        setCreateBy(settingKey.getCreateBy());
        setUpdateTime(settingKey.getUpdateTime());
        setUpdateBy(settingKey.getUpdateBy());
        return this;
    }
}
