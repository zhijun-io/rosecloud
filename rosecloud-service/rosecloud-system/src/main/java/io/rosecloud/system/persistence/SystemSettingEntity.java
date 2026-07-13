package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import io.rosecloud.common.core.model.ToData;
import io.rosecloud.common.core.model.ToEntity;
import io.rosecloud.system.domain.SystemSetting;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@TableName("sys_system_setting")
@Getter
@Setter
@NoArgsConstructor
public class SystemSettingEntity implements ToData<SystemSetting>, ToEntity<SystemSetting, SystemSettingEntity> {

    @TableId(value = "setting_key", type = IdType.INPUT)
    private String settingKey;

    private String value;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("updated_by")
    private Long updatedBy;

    @Override
    public SystemSetting toData() {
        return new SystemSetting(settingKey, value, updatedAt, updatedBy);
    }

    @Override
    public SystemSettingEntity toEntity(SystemSetting setting) {
        setSettingKey(setting.getKey());
        setValue(setting.getValue());
        setUpdatedAt(setting.getUpdatedAt());
        setUpdatedBy(setting.getUpdatedBy());
        return this;
    }
}
